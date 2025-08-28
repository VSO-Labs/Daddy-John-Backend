package com.vso.DaddyJohn.Service;

import com.vso.DaddyJohn.Dto.MessageDto;
import com.vso.DaddyJohn.Entity.Conversation;
import com.vso.DaddyJohn.Entity.Message;
import com.vso.DaddyJohn.Entity.Users;
import com.vso.DaddyJohn.Repositry.ConversationRepo;
import com.vso.DaddyJohn.Repositry.MessageRepo;
import com.vso.DaddyJohn.Repositry.UserRepo;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

    private final MessageRepo messageRepo;
    private final ConversationRepo conversationRepo;
    private final UserRepo userRepo;
    // We will inject the Django API client and UsageService here later.

    public MessageService(MessageRepo messageRepo, ConversationRepo conversationRepo, UserRepo userRepo) {
        this.messageRepo = messageRepo;
        this.conversationRepo = conversationRepo;
        this.userRepo = userRepo;
    }

    /**
     * Get all messages for a specific conversation with pagination.
     */
    public Page<MessageDto> getAllMessagesForConversation(ObjectId conversationId, String username, Pageable pageable) {
        Users user = findUserByUsername(username);
        Conversation conversation = findConversationById(conversationId);

        if (!conversation.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to view these messages.");
        }

        Page<Message> messages = messageRepo.findByConversationId(conversationId, pageable);
        return messages.map(this::convertToDto);
    }

    /**
     * Handles posting a new message from a user, getting a response from the AI, and saving both.
     * This is a placeholder and will be expanded significantly.
     */
    public MessageDto postNewMessage(ObjectId conversationId, String content, String username) {
        Users user = findUserByUsername(username);
        Conversation conversation = findConversationById(conversationId);

        if (!conversation.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to post in this conversation.");
        }

        // Step 1: Check user's subscription and usage limits (to be implemented)
        // For now, we'll proceed without checks.

        // Step 2: Save the user's message
        Message userMessage = new Message();
        userMessage.setConversation(conversation);
        userMessage.setRole(Message.Role.USER);
        userMessage.setContent(content);
        userMessage.setTokenCount(content.length() / 4); // Simple token estimation
        messageRepo.save(userMessage);

        // Step 3: Call the external Django Chatbot API (to be implemented)
        // String aiResponseContent = djangoApiClient.getChatResponse(content);
        // int responseTokenCount = aiResponseContent.length() / 4;
        String aiResponseContent = "This is a placeholder response from the AI."; // Placeholder
        int responseTokenCount = aiResponseContent.length() / 4; // Placeholder

        // Step 4: Save the assistant's response
        Message assistantMessage = new Message();
        assistantMessage.setConversation(conversation);
        assistantMessage.setRole(Message.Role.ASSISTANT);
        assistantMessage.setContent(aiResponseContent);
        assistantMessage.setTokenCount(responseTokenCount);
        Message savedAssistantMessage = messageRepo.save(assistantMessage);

        // Step 5: Update the user's daily token and message count (to be implemented)
        // usageService.recordUsage(user.getId(), 1, userMessage.getTokenCount() + responseTokenCount);

        return convertToDto(savedAssistantMessage);
    }

    // --- Helper Methods ---

    private Users findUserByUsername(String username) {
        Users user = userRepo.findByUsername(username);
        if (user == null) {
            throw new IllegalStateException("Authenticated user not found in database.");
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
        return dto;
    }
}

