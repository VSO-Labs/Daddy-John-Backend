package com.vso.DaddyJohn.Controller;

import com.vso.DaddyJohn.Dto.ConversationDto;
import com.vso.DaddyJohn.Dto.CreateConversationRequest;
import com.vso.DaddyJohn.Dto.UpdateConversationTitleRequest;
import com.vso.DaddyJohn.Service.ConversationService;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Handles all API requests related to conversations.
 * Use Case: Manages the creation, retrieval, updating, and deletion of user chat histories.
 */
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /**
     * Retrieves a paginated list of all conversations for the authenticated user.
     * Use Case: To display the user's chat history in a sidebar.
     */
    @GetMapping
    public Page<ConversationDto> getAllConversationsForUser(Authentication authentication, Pageable pageable) {
        return conversationService.getAllConversationsForUser(authentication.getName(), pageable);
    }

    /**
     * Creates a new, empty conversation for the authenticated user.
     * Use Case: When a user starts a new chat from the main screen.
     */
    @PostMapping
    public ConversationDto createConversation(Authentication authentication, @RequestBody CreateConversationRequest request) {
        return conversationService.createConversation(authentication.getName(), request.getTitle());
    }

    /**
     * Updates the title of a specific conversation.
     * Use Case: To allow a user to rename a chat session for better organization.
     */
    @PutMapping("/{id}")
    public ConversationDto updateConversationTitle(Authentication authentication, @PathVariable String id, @RequestBody UpdateConversationTitleRequest request) {
        if (!ObjectId.isValid(id)) {
            throw new IllegalArgumentException("Invalid ID format. Please provide a valid 24-character hex string.");
        }
        return conversationService.updateConversationTitle(new ObjectId(id), request.getTitle(), authentication.getName());
    }

    /**
     * Deletes a specific conversation and all its associated messages.
     * Use Case: When a user wants to permanently remove a chat from their history.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(Authentication authentication, @PathVariable String id) {
        if (!ObjectId.isValid(id)) {
            throw new IllegalArgumentException("Invalid ID format. Please provide a valid 24-character hex string.");
        }
        conversationService.deleteConversation(new ObjectId(id), authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
