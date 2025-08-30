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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class MessageService {

    private final MessageRepo messageRepo;
    private final ConversationRepo conversationRepo;
    private final UserRepo userRepo;
    private final UsageService usageService;
    private final RestTemplate restTemplate;

    // Injected from application.yml
    @Value("${services.chatbot.django-url}")
    private String djangoApiUrl;

    public MessageService(MessageRepo messageRepo, ConversationRepo conversationRepo, UserRepo userRepo, UsageService usageService, RestTemplate restTemplate) {
        this.messageRepo = messageRepo;
        this.conversationRepo = conversationRepo;
        this.userRepo = userRepo;
        this.usageService = usageService;
        this.restTemplate = restTemplate;
    }

    public MessageDto postNewMessage(ObjectId conversationId, String content, String username) {
        Users user = findUserByUsername(username);
        Conversation conversation = findConversationById(conversationId);

        if (!conversation.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to post in this conversation.");
        }

        if (!usageService.canSendMessage(user)) {
            throw new RuntimeException("Message limit exceeded for your current plan.");
        }

        Message userMessage = new Message();
        userMessage.setConversation(conversation);
        userMessage.setRole(Message.Role.USER);
        userMessage.setContent(content);
        userMessage.setTokenCount(countTokens(content));
        messageRepo.save(userMessage);

        // --- Call the custom Django API ---
        String aiResponseContent;
        int responseTokenCount;
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("message", content);

            // Assuming the Django API returns a JSON like: {"response": "...", "token_count": 123}
            ResponseEntity<Map> apiResponse = restTemplate.postForEntity(djangoApiUrl, requestBody, Map.class);

            Map<String, Object> responseBody = apiResponse.getBody();
            aiResponseContent = (String) responseBody.getOrDefault(     "response", "Sorry, I couldn't get a response.");
            // Ensure token count is handled as an Integer, as JSON numbers are often parsed as such
            responseTokenCount = (Integer) responseBody.getOrDefault("token_count", countTokens(aiResponseContent));

        } catch (Exception e) {
            aiResponseContent = "Sorry, an error occurred while connecting to the chatbot service.";
            responseTokenCount = countTokens(aiResponseContent);
        }

        Message assistantMessage = new Message();
        assistantMessage.setConversation(conversation);
        assistantMessage.setRole(Message.Role.ASSISTANT);
        assistantMessage.setContent(aiResponseContent);
        assistantMessage.setTokenCount(responseTokenCount);
        Message savedAssistantMessage = messageRepo.save(assistantMessage);

        usageService.recordUsage(user, userMessage.getTokenCount() + responseTokenCount);

        return convertToDto(savedAssistantMessage);
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
        return userRepo.findByUsername(username);
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
        return dto;
    }
}