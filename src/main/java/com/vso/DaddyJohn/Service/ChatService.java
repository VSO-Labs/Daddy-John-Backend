package com.vso.DaddyJohn.Service;

import com.vso.DaddyJohn.Entity.Conversation;
import com.vso.DaddyJohn.Entity.Message;
import com.vso.DaddyJohn.Entity.Users;
import com.vso.DaddyJohn.Repositry.ConversationRepo;
import com.vso.DaddyJohn.Repositry.MessageRepo;
import com.vso.DaddyJohn.Repositry.UserRepo;
import lombok.AllArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class ChatService {

    private final ConversationRepo conversationRepository;
    private final MessageRepo messageRepository;
    private final UsageService usageService;
    private final RestTemplate restTemplate; // Assumes a RestTemplate bean is configured

    public Conversation createConversation(Users user, String title) {
        Conversation conversation = new Conversation();
        conversation.setUserId(user.getId());
        conversation.setTitle(title);
        return conversationRepository.save(conversation);
    }

    public Message postNewMessage(Users user, ObjectId conversationId, String userContent) {
        // 1. Check if the user is allowed to send a message
        if (!usageService.canSendMessage(user)) {
            throw new RuntimeException("Usage limit exceeded. Please upgrade your plan.");
        }

        // 2. Verify conversation ownership
        Conversation conversation = conversationRepository.findById(conversationId)
                .filter(c -> c.getUserId().equals(user.getId()))
                .orElseThrow(() -> new SecurityException("Conversation not found or access denied."));

        // 3. Save the user's message
        Message userMessage = new Message();
        userMessage.setConversationId(conversationId);
        userMessage.setRole(Message.Role.valueOf("user"));
        userMessage.setContent(userContent);
        messageRepository.save(userMessage);

        // 4. Call the external Django Chatbot API
        String djangoApiUrl = "http://your-django-api-url/chat"; // Replace with your actual URL
        Map<String, String> requestBody = Map.of("prompt", userContent);
        // This is a placeholder response structure
        Map<String, Object> apiResponse = restTemplate.postForObject(djangoApiUrl, requestBody, Map.class);

        String assistantContent = (String) apiResponse.getOrDefault("response", "Error: Could not get response.");
        int tokenCount = (Integer) apiResponse.getOrDefault("token_count", 0);

        // 5. Save the assistant's message
        Message assistantMessage = new Message();
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setRole(Message.Role.valueOf("assistant"));
        assistantMessage.setContent(assistantContent);
        assistantMessage.setTokenCount(tokenCount);
        messageRepository.save(assistantMessage);

        // 6. Record the usage
        usageService.recordUsage(user, tokenCount);

        // 7. Update conversation timestamp
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return assistantMessage;
    }

    public List<Conversation> getUserConversations(Users user) {
        return ConversationService.getAllConversationsForUser()
    }

    public List<Message> getMessagesInConversation(Users user, ObjectId conversationId) {
        // Verify conversation ownership before returning messages
        conversationRepository.findById(conversationId)
                .filter(c -> c.getUserId().equals(user.getId()))
                .orElseThrow(() -> new SecurityException("Conversation not found or access denied."));

        return messageRepository.findByConversationId(conversationId);
    }
}