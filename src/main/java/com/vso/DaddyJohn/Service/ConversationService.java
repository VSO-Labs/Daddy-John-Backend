package com.vso.DaddyJohn.Service;

import com.vso.DaddyJohn.Dto.ConversationDto;
import com.vso.DaddyJohn.Entity.Conversation;
import com.vso.DaddyJohn.Entity.Users;
import com.vso.DaddyJohn.Repositry.ConversationRepo;
import com.vso.DaddyJohn.Repositry.MessageRepo;
import com.vso.DaddyJohn.Repositry.UserRepo;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ConversationService {

    private final ConversationRepo conversationRepo;
    private final UserRepo userRepo;
    private final MessageRepo messageRepo;

    public ConversationService(ConversationRepo conversationRepo, UserRepo userRepo, MessageRepo messageRepo) {
        this.conversationRepo = conversationRepo;
        this.userRepo = userRepo;
        this.messageRepo = messageRepo;
    }

    /**
       Get all conversations for the currently authenticated user with pagination.
     */
    public Page<ConversationDto> getAllConversationsForUser(String username, Pageable pageable) {
        Users user = findUserByUsername(username);
        Page<Conversation> conversations = conversationRepo.findByUserId(user.getId(), pageable);
        return conversations.map(this::convertToDto);
    }

    /**
     * Create a new conversation for the currently authenticated user.
     */
    public ConversationDto createConversation(String username, String title) {
        Users user = findUserByUsername(username);
        Conversation conversation = new Conversation();
        conversation.setUser(user);
        conversation.setTitle(title);
        Conversation savedConversation = conversationRepo.save(conversation);
        return convertToDto(savedConversation);
    }

    /**
     * Update the title of a conversation, ensuring the user owns it.
     */
    public ConversationDto updateConversationTitle(ObjectId conversationId, String newTitle, String username) {
        Users user = findUserByUsername(username);
        Conversation conversation = findConversationById(conversationId);

        if (!conversation.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to update this conversation.");
        }

        conversation.setTitle(newTitle);
        conversation.setUpdatedAt(LocalDateTime.now());
        Conversation updatedConversation = conversationRepo.save(conversation);
        return convertToDto(updatedConversation);
    }

    /**
     * Delete a conversation and all its messages, ensuring the user owns it.
     */
    @Transactional
    public void deleteConversation(ObjectId conversationId, String username) {
        Users user = findUserByUsername(username);
        Conversation conversation = findConversationById(conversationId);

        if (!conversation.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to delete this conversation.");
        }

        // First, delete all messages associated with the conversation
        messageRepo.deleteAllByConversationId(conversationId);

        // Then, delete the conversation itself
        conversationRepo.delete(conversation);
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

    private ConversationDto convertToDto(Conversation conversation) {
        ConversationDto dto = new ConversationDto();
        dto.setId(conversation.getId().toHexString());
        dto.setTitle(conversation.getTitle());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        return dto;
    }
}
