package com.vso.DaddyJohn.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vso.DaddyJohn.Dto.MessageDto;
import com.vso.DaddyJohn.Entity.Conversation;
import com.vso.DaddyJohn.Entity.Message;
import com.vso.DaddyJohn.Entity.Users;
import com.vso.DaddyJohn.Exception.TokenLimitExceededException;
import com.vso.DaddyJohn.Repositry.ConversationRepo;
import com.vso.DaddyJohn.Repositry.MessageRepo;
import com.vso.DaddyJohn.Repositry.UserRepo;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessageService {

    private final MessageRepo messageRepo;
    private final ConversationRepo conversationRepo;
    private final UserRepo userRepo;
    private final TokenUsageService tokenUsageService;
    private final RestTemplate restTemplate;
    private final FileStorageService fileStorageService;

    @Value("${services.chatbot.django-url}")
    private String djangoApiUrl;

    public MessageService(MessageRepo messageRepo, ConversationRepo conversationRepo,
                          UserRepo userRepo, TokenUsageService tokenUsageService, RestTemplate restTemplate,
                          FileStorageService fileStorageService, ObjectMapper objectMapper) {
        this.messageRepo = messageRepo;
        this.conversationRepo = conversationRepo;
        this.userRepo = userRepo;
        this.tokenUsageService = tokenUsageService;
        this.restTemplate = restTemplate;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    /**
     * Posts a new message with optional photo attachments
     */
    public MessageDto postNewMessage(ObjectId conversationId, String content,
                                     String username, List<MultipartFile> photos) {
        Users user = findUserByUsername(username);
        Conversation conversation = findConversationById(conversationId);

        if (!conversation.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to post in this conversation.");
        }

        // Check token limit BEFORE making the API call
        if (!tokenUsageService.canSendMessage(user, content)) {
            Map<String, Object> tokenStatus = tokenUsageService.getUserTokenStatus(user);
            throw new TokenLimitExceededException(
                    "Token limit exceeded. You have used " + tokenStatus.get("tokensUsed") +
                            " out of " + tokenStatus.get("tokensLimit") + " tokens this month."
            );
        }

        List<String> photoUrls = new ArrayList<>();
        if (photos != null && !photos.isEmpty()) {
            photoUrls = validateAndStorePhotos(photos);
        }

        // Save user message
        Message userMessage = new Message();
        userMessage.setConversation(conversation);
        userMessage.setRole(Message.Role.USER);
        userMessage.setContent(content);
        userMessage.setTokenCount(content.length() / 4); // Simple token count for user message
        userMessage.setPhotoUrls(photoUrls);
        messageRepo.save(userMessage);

        // Get AI response
        String aiResponseContent;
        try {
            Map<String, Object> response;
            if (photos != null && !photos.isEmpty()) {
                response = callChatbotWithPhotos(content, photos);
            } else {
                response = callChatbotWithText(content);
            }
            aiResponseContent = (String) response.getOrDefault("response", "Sorry, I couldn't process your request.");
        } catch (Exception e) {
            aiResponseContent = "Sorry, an error occurred while connecting to the chatbot service: " + e.getMessage();
        }

        // Calculate actual tokens used
        int tokensUsed = tokenUsageService.calculateTokens(content, aiResponseContent);

        // Save assistant message
        Message assistantMessage = new Message();
        assistantMessage.setConversation(conversation);
        assistantMessage.setRole(Message.Role.ASSISTANT);
        assistantMessage.setContent(aiResponseContent);
        assistantMessage.setTokenCount(tokensUsed);
        Message savedAssistantMessage = messageRepo.save(assistantMessage);

        // Record token usage
        tokenUsageService.recordTokenUsage(user, tokensUsed);

        return convertToDto(savedAssistantMessage);
    }

    public MessageDto postNewMessage(ObjectId conversationId, String content, String username) {
        return postNewMessage(conversationId, content, username, null);
    }

    private Map<String, Object> callChatbotWithText(String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("user_input", content);
        requestBody.put("history", Collections.emptyList());
        requestBody.put("latest_summary", null);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        return restTemplate.postForObject(djangoApiUrl, entity, Map.class);
    }

    private Map<String, Object> callChatbotWithPhotos(String content, List<MultipartFile> photos) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        Map<String, Object> jsonPayload = new HashMap<>();
        jsonPayload.put("user_input", content);
        jsonPayload.put("history", Collections.emptyList());
        jsonPayload.put("latest_summary", null);

        body.add("metadata", new HttpEntity<>(jsonPayload, createJsonHeaders()));

        for (MultipartFile photo : photos) {
            ByteArrayResource photoResource = new ByteArrayResource(photo.getBytes()) {
                @Override
                public String getFilename() {
                    return photo.getOriginalFilename();
                }
            };
            body.add("photos", photoResource);
        }

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        return restTemplate.postForObject(djangoApiUrl, entity, Map.class);
    }

    private HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private List<String> validateAndStorePhotos(List<MultipartFile> photos) {
        List<String> photoUrls = new ArrayList<>();

        if (photos.size() > 5) {
            throw new RuntimeException("Maximum 5 photos allowed per message.");
        }

        for (MultipartFile photo : photos) {
            if (!isValidImageType(photo)) {
                throw new RuntimeException("Only image files (JPG, JPEG, PNG, GIF, WEBP) are allowed.");
            }
            if (photo.getSize() > 10 * 1024 * 1024) {
                throw new RuntimeException("Photo size must be less than 10MB.");
            }
            try {
                String photoUrl = fileStorageService.storeFile(photo);
                photoUrls.add(photoUrl);
            } catch (Exception e) {
                throw new RuntimeException("Failed to store photo: " + e.getMessage());
            }
        }
        return photoUrls;
    }

    private boolean isValidImageType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (
                contentType.equals("image/jpeg") ||
                        contentType.equals("image/jpg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/gif") ||
                        contentType.equals("image/webp")
        );
    }

    public Page<MessageDto> getAllMessagesForConversation(ObjectId conversationId, String username, Pageable pageable) {
        Users user = findUserByUsername(username);
        Conversation conversation = findConversationById(conversationId);

        if (!conversation.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to view these messages.");
        }
        return messageRepo.findByConversation_Id(conversationId, pageable).map(this::convertToDto);
    }

    private Users findUserByUsername(String username) {
        Users user = userRepo.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }
        return user;
    }

    private Conversation findConversationById(ObjectId conversationId) {
        return conversationRepo.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found with ID: " + conversationId));
    }

    private MessageDto convertToDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId().toHexString());
        dto.setRole(message.getRole());
        dto.setContent(message.getContent());
        dto.setTokenCount(message.getTokenCount());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setPhotoUrls(message.getPhotoUrls());
        return dto;
    }
}