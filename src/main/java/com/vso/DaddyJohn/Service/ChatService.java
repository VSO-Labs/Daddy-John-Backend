package com.vso.DaddyJohn.Service;

import com.vso.DaddyJohn.Dto.ConversationDto;
import com.vso.DaddyJohn.Entity.Conversation;
import com.vso.DaddyJohn.Entity.Message;
import com.vso.DaddyJohn.Entity.Users;
import com.vso.DaddyJohn.Repositry.ConversationRepo;
import com.vso.DaddyJohn.Repositry.MessageRepo;
import com.vso.DaddyJohn.Repositry.UserRepo;
import lombok.AllArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
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
    private final RestTemplate restTemplate;
    private final ConversationService conversationService;

    public Conversation createConversation(Users user, String title) {
        Conversation conversation = new Conversation();
        conversation.setUser(user);
        conversation.setTitle(title);
        return conversationRepository.save(conversation);
    }

    public Message postNewMessage(Users user, ObjectId conversationId, String userContent) {
        if (!usageService.canSendMessage(user)) {
            throw new RuntimeException("Usage limit exceeded. Please upgrade your plan.");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new SecurityException("Conversation not found or access denied."));

        Message userMessage = new Message();
        userMessage.setConversation(conversation);
        userMessage.setRole(Message.Role.USER);
        userMessage.setContent(userContent);
        messageRepository.save(userMessage);

        String djangoApiUrl = "http://your-django-api-url/chat";
        Map<String, String> requestBody = Map.of("prompt", userContent);
        Map<String, Object> apiResponse = restTemplate.postForObject(djangoApiUrl, requestBody, Map.class);

        String assistantContent = (String) apiResponse.getOrDefault("response", "Error: Could not get response.");
        int tokenCount = (Integer) apiResponse.getOrDefault("token_count", 0);

        Message assistantMessage = new Message();
        assistantMessage.setConversation(conversation);
        assistantMessage.setRole(Message.Role.ASSISTANT);
        assistantMessage.setContent(assistantContent);
        assistantMessage.setTokenCount(tokenCount);
        messageRepository.save(assistantMessage);

        usageService.recordUsage(user, tokenCount);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return assistantMessage;
    }

    public List<ConversationDto> getUserConversations(Users user) {
        return conversationService.getAllConversationsForUser(user.getUsername(), Pageable.unpaged()).getContent();
    }

    public List<Message> getMessagesInConversation(Users user, ObjectId conversationId) {
        conversationRepository.findById(conversationId)
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new SecurityException("Conversation not found or access denied."));

        return messageRepository.findAllById(List.of(conversationId));
    }
}