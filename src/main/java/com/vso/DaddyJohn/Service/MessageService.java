package com.vso.DaddyJohn.Service;

import com.vso.DaddyJohn.Dto.MessageDto;
import com.vso.DaddyJohn.Entity.*;
import com.vso.DaddyJohn.Repositry.*;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer for handling all message-related operations.
 * Manages the core chat functionality including AI integration.
 */
@Service
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    private static final int MAX_HISTORY_MESSAGES = 10; // Limit history for context
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final MessageRepo messageRepo;
    private final ConversationRepo conversationRepo;
    private final UserRepo userRepo;
    private final UsageService usageService;
    private final FileStorageService fileStorageService;
    private final ApiAccessLogRepo apiAccessLogRepo;
    private final RestTemplate restTemplate;

    @Value("${services.chatbot.django-url}")
    private String djangoApiUrl;

    public MessageService(
            MessageRepo messageRepo,
            ConversationRepo conversationRepo,
            UserRepo userRepo,
            UsageService usageService,
            FileStorageService fileStorageService,
            ApiAccessLogRepo apiAccessLogRepo,
            RestTemplate restTemplate) {
        this.messageRepo = messageRepo;
        this.conversationRepo = conversationRepo;
        this.userRepo = userRepo;
        this.usageService = usageService;
        this.fileStorageService = fileStorageService;
        this.apiAccessLogRepo = apiAccessLogRepo;
        this.restTemplate = restTemplate;
    }

    /**
     * Retrieves paginated messages for a conversation.
     */
    public Page<MessageDto> getMessagesForConversation(ObjectId conversationId, String username, Pageable pageable) {
        validateUserOwnsConversation(conversationId, username);

        // Sort messages by creation time (oldest first)
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("createdAt").ascending()
        );

        Page<Message> messages = messageRepo.findByConversationId(conversationId, sortedPageable);
        return messages.map(this::convertToDto);
    }

    /**
     * Sends a message (with optional photos) and gets AI response.
     */
    @Transactional
    public MessageDto sendMessage(ObjectId conversationId, String username, String content, List<MultipartFile> photos) {
        Users user = findUserByUsername(username);
        Conversation conversation = validateUserOwnsConversation(conversationId, username);

        // Check usage limits
        if (!usageService.canSendMessage(user)) {
            throw new RuntimeException("Daily message limit reached. Please upgrade your plan or wait until tomorrow.");
        }

        // Save user message
        Message userMessage = createUserMessage(conversation, content, photos);
        Message savedUserMessage = messageRepo.save(userMessage);
        logger.info("User message saved with ID: {}", savedUserMessage.getId());

        // Update conversation timestamp
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepo.save(conversation);

        // Get AI response
        Message aiResponse = getAIResponse(conversation, user, content);
        Message savedAiResponse = messageRepo.save(aiResponse);
        logger.info("AI response saved with ID: {}", savedAiResponse.getId());

        // Record usage
        int totalTokens = (userMessage.getTokenCount() != null ? userMessage.getTokenCount() : 0) +
                (aiResponse.getTokenCount() != null ? aiResponse.getTokenCount() : 0);
        usageService.recordUsage(user, totalTokens);

        // Log API access
        logApiAccess(user, conversation, content, aiResponse.getContent(), totalTokens);

        return convertToDto(savedAiResponse);
    }

    /**
     * Gets a specific message by ID.
     */
    public MessageDto getMessageById(ObjectId messageId, ObjectId conversationId, String username) {
        validateUserOwnsConversation(conversationId, username);

        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getConversation().getId().equals(conversationId)) {
            throw new AccessDeniedException("Message does not belong to this conversation");
        }

        return convertToDto(message);
    }

    /**
     * Deletes a message.
     */
    @Transactional
    public void deleteMessage(ObjectId messageId, ObjectId conversationId, String username) {
        validateUserOwnsConversation(conversationId, username);

        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getConversation().getId().equals(conversationId)) {
            throw new AccessDeniedException("Message does not belong to this conversation");
        }

        // Delete associated files if any
        if (message.getPhotoUrls() != null) {
            for (String photoUrl : message.getPhotoUrls()) {
                String filename = photoUrl.substring(photoUrl.lastIndexOf("/") + 1);
                fileStorageService.deleteFile(filename);
            }
        }

        messageRepo.delete(message);
        logger.info("Message {} deleted from conversation {}", messageId, conversationId);
    }

    /**
     * Gets conversation summary for AI context.
     */
    public Map<String, Object> getConversationSummary(ObjectId conversationId, String username) {
        validateUserOwnsConversation(conversationId, username);

        // Get recent messages for context
        Pageable pageable = PageRequest.of(0, MAX_HISTORY_MESSAGES, Sort.by("createdAt").descending());
        Page<Message> recentMessages = messageRepo.findByConversationId(conversationId, pageable);

        List<Map<String, Object>> history = recentMessages.getContent().stream()
                .sorted(Comparator.comparing(Message::getCreatedAt))
                .map(msg -> {
                    Map<String, Object> historyItem = new HashMap<>();
                    historyItem.put("role", msg.getRole().toString());
                    historyItem.put("content", msg.getContent());
                    historyItem.put("timestamp", msg.getCreatedAt().toString());
                    return historyItem;
                })
                .collect(Collectors.toList());

        Map<String, Object> summary = new HashMap<>();
        summary.put("conversationId", conversationId.toHexString());
        summary.put("messageCount", recentMessages.getTotalElements());
        summary.put("history", history);
        summary.put("latestSummary", null); // Could be implemented for long conversations

        return summary;
    }

    // --- Private Helper Methods ---

    private Message createUserMessage(Conversation conversation, String content, List<MultipartFile> photos) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setRole(Message.Role.USER);
        message.setContent(content != null ? content : "");
        message.setTokenCount(estimateTokenCount(content));

        if (photos != null && !photos.isEmpty()) {
            List<String> photoUrls = new ArrayList<>();
            List<Message.FileMetadata> attachments = new ArrayList<>();

            for (MultipartFile photo : photos) {
                try {
                    String photoUrl = fileStorageService.storeFile(photo);
                    photoUrls.add(photoUrl);

                    Message.FileMetadata metadata = new Message.FileMetadata();
                    metadata.setOriginalName(photo.getOriginalFilename());
                    metadata.setStoredName(photoUrl.substring(photoUrl.lastIndexOf("/") + 1));
                    metadata.setFileType(photo.getContentType());
                    metadata.setFileSize(photo.getSize());
                    metadata.setUrl(photoUrl);
                    attachments.add(metadata);
                } catch (IOException e) {
                    logger.error("Failed to store photo: {}", e.getMessage());
                    throw new RuntimeException("Failed to upload photo: " + e.getMessage());
                }
            }

            message.setPhotoUrls(photoUrls);
            message.setAttachments(attachments);
            message.setMessageType(content != null && !content.isEmpty()
                    ? Message.MessageType.TEXT_WITH_IMAGE
                    : Message.MessageType.IMAGE);
        } else {
            message.setMessageType(Message.MessageType.TEXT);
        }

        return message;
    }

    private Message getAIResponse(Conversation conversation, Users user, String userInput) {
        // Prepare conversation history
        List<Map<String, Object>> history = prepareConversationHistory(conversation.getId());

        // Call Django API with retries
        Map<String, Object> aiResponse = callDjangoAPIWithRetry(userInput, history);

        // Create AI message
        Message aiMessage = new Message();
        aiMessage.setConversation(conversation);
        aiMessage.setRole(Message.Role.ASSISTANT);
        aiMessage.setContent((String) aiResponse.get("response"));
        aiMessage.setTokenCount((Integer) aiResponse.getOrDefault("token_count", estimateTokenCount(aiMessage.getContent())));
        aiMessage.setMessageType(Message.MessageType.TEXT);

        return aiMessage;
    }

    private Map<String, Object> callDjangoAPIWithRetry(String userInput, List<Map<String, Object>> history) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return callDjangoAPI(userInput, history);
            } catch (Exception e) {
                lastException = e;
                logger.warn("Django API call attempt {} failed: {}", attempt, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while retrying", ie);
                    }
                }
            }
        }

        logger.error("All Django API call attempts failed", lastException);
        // Return a fallback response
        return createFallbackResponse();
    }

    private Map<String, Object> callDjangoAPI(String userInput, List<Map<String, Object>> history) {
        try {
            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("user_input", userInput != null ? userInput : "");
            requestBody.put("history", history);
            requestBody.put("latest_summary", null);

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            logger.debug("Calling Django API at: {} with body: {}", djangoApiUrl, requestBody);

            // Make API call
            ResponseEntity<Map> response = restTemplate.postForEntity(djangoApiUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.debug("Django API response: {}", response.getBody());
                return response.getBody();
            } else {
                throw new RuntimeException("Django API returned non-success status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error calling Django API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get AI response: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> createFallbackResponse() {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("response", "I apologize, but I'm having trouble processing your request at the moment. Please try again in a few seconds.");
        fallback.put("token_count", 20);
        return fallback;
    }

    private List<Map<String, Object>> prepareConversationHistory(ObjectId conversationId) {
        // Get recent messages for context
        Pageable pageable = PageRequest.of(0, MAX_HISTORY_MESSAGES, Sort.by("createdAt").descending());
        Page<Message> recentMessages = messageRepo.findByConversationId(conversationId, pageable);

        return recentMessages.getContent().stream()
                .sorted(Comparator.comparing(Message::getCreatedAt))
                .map(msg -> {
                    Map<String, Object> historyItem = new HashMap<>();
                    historyItem.put("role", msg.getRole().toString().toLowerCase());
                    historyItem.put("content", msg.getContent());
                    return historyItem;
                })
                .collect(Collectors.toList());
    }

    private void logApiAccess(Users user, Conversation conversation, String request, String response, int tokensUsed) {
        try {
            ApiAccessLog log = new ApiAccessLog();
            log.setUser(user);
            log.setEndpoint("/api/conversations/" + conversation.getId() + "/messages");
            log.setRequestPayload(Map.of("content", request != null ? request : ""));
            log.setResponsePayload(Map.of("content", response != null ? response : ""));
            log.setStatusCode(200);
            log.setTokensUsed(tokensUsed);
            apiAccessLogRepo.save(log);
        } catch (Exception e) {
            logger.error("Failed to log API access: {}", e.getMessage());
        }
    }

    private Conversation validateUserOwnsConversation(ObjectId conversationId, String username) {
        Users user = findUserByUsername(username);
        Conversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (!conversation.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to access this conversation");
        }

        return conversation;
    }

    private Users findUserByUsername(String username) {
        Users user = userRepo.findByUsername(username);
        if (user == null) {
            throw new IllegalStateException("Authenticated user not found in database");
        }
        return user;
    }

    private MessageDto convertToDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId().toHexString());
        dto.setRole(message.getRole());
        dto.setContent(message.getContent());
        dto.setTokenCount(message.getTokenCount());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setPhotoUrls(message.getPhotoUrls());
        dto.setMessageType(message.getMessageType());
        dto.setAttachments(message.getAttachments());
        return dto;
    }

    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // Rough estimation: 1 token ≈ 4 characters
        return (int) Math.ceil(text.length() / 4.0);
    }
}