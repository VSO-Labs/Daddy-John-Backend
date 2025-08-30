package com.vso.DaddyJohn.Controller;

import com.vso.DaddyJohn.Dto.CreateMessageRequest;
import com.vso.DaddyJohn.Dto.MessageDto;
import com.vso.DaddyJohn.Service.MessageService;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Handles all API requests for messages within a specific conversation.
 * Use Case: Manages the sending and receiving of chat messages.
 */
@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Retrieves a paginated list of all messages within a given conversation.
     * Use Case: To load and display the chat history when a user opens a conversation.
     */
    @GetMapping
    public Page<MessageDto> getAllMessagesForConversation(Authentication authentication, @PathVariable String conversationId, Pageable pageable) {
        if (!ObjectId.isValid(conversationId)) {
            throw new IllegalArgumentException("Invalid Conversation ID format. Please provide a valid 24-character hex string.");
        }
        return messageService.getAllMessagesForConversation(new ObjectId(conversationId), authentication.getName(), pageable);
    }

    /**
     * Posts a new message from the user to a conversation and gets the AI's response.
     * Use Case: When a user types a message and hits 'send' in the chat window.
     */
    @PostMapping
    public MessageDto postNewMessage(Authentication authentication, @PathVariable String conversationId, @RequestBody CreateMessageRequest request) {
        if (!ObjectId.isValid(conversationId)) {
            throw new IllegalArgumentException("Invalid Conversation ID format. Please provide a valid 24-character hex string.");
        }
        return messageService.postNewMessage(new ObjectId(conversationId), request.getContent(), authentication.getName());
    }
}
