package com.vso.DaddyJohn.Service;

import com.vso.DaddyJohn.Dto.MessageDto;
import com.vso.DaddyJohn.Entity.Conversation;
import com.vso.DaddyJohn.Entity.Message;
import com.vso.DaddyJohn.Entity.Users;
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
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Service
public class MessageService {

    private final MessageRepo messageRepo;
    private final ConversationRepo conversationRepo;
    private final UserRepo userRepo;
    private final UsageService usageService;
    private final RestTemplate restTemplate;
    private final FileStorageService fileStorageService;

    // Injected from application.yml
    @Value("${services.chatbot.django-url}")
    private String djangoApiUrl;

    public MessageService(MessageRepo messageRepo, ConversationRepo conversationRepo,
                          UserRepo userRepo, UsageService usageService, RestTemplate restTemplate,
                          FileStorageService fileStorageService) {
        this.messageRepo = messageRepo;
        this.conversationRepo = conversationRepo;
        this.userRepo = userRepo;
        this.usageService = usageService;
        this.restTemplate = restTemplate;
        this.fileStorageService = fileStorageService;
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

        if (!usageService.canSendMessage(user)) {
            throw new RuntimeException("Message limit exceeded for your current plan.");
        }

        // Validate and process photos
        List<String> photoUrls = new ArrayList<>();
        if (photos != null && !photos.isEmpty()) {
            photoUrls = validateAndStorePhotos(photos);
        }

        // Create user message
        Message userMessage = new Message();
        userMessage.setConversation(conversation);
        userMessage.setRole(Message.Role.USER);
        userMessage.setContent(content);
        userMessage.setTokenCount(countTokens(content));
        userMessage.setPhotoUrls(photoUrls); // Add photo URLs to message
        messageRepo.save(userMessage);

        // Call chatbot API
        String aiResponseContent;
        int responseTokenCount;
        try {
            if (!photoUrls.isEmpty()) {
                // Send photos along with text to chatbot
                Map<String, Object> response = callChatbotWithPhotos(content, photos);
                aiResponseContent = (String) response.getOrDefault("response", "Sorry, I couldn't process your request.");
                responseTokenCount = (Integer) response.getOrDefault("token_count", countTokens(aiResponseContent));
            } else {
                // Text-only message
                Map<String, Object> response = callChatbotWithText(content);
                aiResponseContent = (String) response.getOrDefault("response", "Sorry, I couldn't get a response.");
                responseTokenCount = (Integer) response.getOrDefault("token_count", countTokens(aiResponseContent));
            }
        } catch (Exception e) {
            aiResponseContent = "Sorry, an error occurred while connecting to the chatbot service: " + e.getMessage();
            responseTokenCount = countTokens(aiResponseContent);
        }

        // Create assistant message
        Message assistantMessage = new Message();
        assistantMessage.setConversation(conversation);
        assistantMessage.setRole(Message.Role.ASSISTANT);
        assistantMessage.setContent(aiResponseContent);
        assistantMessage.setTokenCount(responseTokenCount);
        Message savedAssistantMessage = messageRepo.save(assistantMessage);

        // Record usage
        usageService.recordUsage(user, userMessage.getTokenCount() + responseTokenCount);

        return convertToDto(savedAssistantMessage);
    }

    /**
     * Overloaded method for backward compatibility (text-only messages)
     */
    public MessageDto postNewMessage(ObjectId conversationId, String content, String username) {
        return postNewMessage(conversationId, content, username, null);
    }

    /**
     * Calls chatbot API with text only
     */
    private Map<String, Object> callChatbotWithText(String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("user_input", content);
        requestBody.put("history", new ArrayList<>()); // empty list for now
        requestBody.put("latest_summary", null);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(djangoApiUrl, entity, Map.class);

        return response.getBody();
    }

    /**
     * Calls chatbot API with photos and text
     */
    private Map<String, Object> callChatbotWithPhotos(String content, List<MultipartFile> photos) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("message", content);
        body.add("type", "image");

        // Add photos to the request
        for (int i = 0; i < photos.size(); i++) {
            MultipartFile photo = photos.get(i);
            ByteArrayResource photoResource = new ByteArrayResource(photo.getBytes()) {
                @Override
                public String getFilename() {
                    return photo.getOriginalFilename();
                }
            };
            body.add("photos", photoResource);
        }

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(djangoApiUrl, entity, Map.class);

        return response.getBody();
    }

    /**
     * Validates and stores uploaded photos
     */
    private List<String> validateAndStorePhotos(List<MultipartFile> photos) {
        List<String> photoUrls = new ArrayList<>();

        // Validate photo count (max 5 photos per message)
        if (photos.size() > 5) {
            throw new RuntimeException("Maximum 5 photos allowed per message.");
        }

        for (MultipartFile photo : photos) {
            // Validate file type
            if (!isValidImageType(photo)) {
                throw new RuntimeException("Only image files (JPG, JPEG, PNG, GIF, WEBP) are allowed.");
            }

            // Validate file size (max 10MB per photo)
            if (photo.getSize() > 10 * 1024 * 1024) {
                throw new RuntimeException("Photo size must be less than 10MB.");
            }

            // Store the photo and get URL
            try {
                String photoUrl = fileStorageService.storeFile(photo);
                photoUrls.add(photoUrl);
            } catch (Exception e) {
                throw new RuntimeException("Failed to store photo: " + e.getMessage());
            }
        }

        return photoUrls;
    }

    /**
     * Validates if the file is a valid image type
     */
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
        return messageRepo.findByConversationId(conversationId, pageable).map(this::convertToDto);
    }

    private int countTokens(String text) {
        return (int) Math.ceil(text.length() / 4.0);
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
        dto.setPhotoUrls(message.getPhotoUrls()); // Include photo URLs in DTO
        return dto;
    }
}