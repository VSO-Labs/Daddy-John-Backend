package com.vso.DaddyJohn.Controller;

import com.vso.DaddyJohn.Dto.CreateMessageRequest;
import com.vso.DaddyJohn.Dto.MessageDto;
import com.vso.DaddyJohn.Service.MessageService;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Handles all API requests related to messages within conversations.
 * Manages chat interactions between users and the AI assistant.
 */
@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
@CrossOrigin(origins = "*", maxAge = 3600)
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Retrieves all messages for a specific conversation with pagination.
     * Use Case: Loading chat history when a user opens a conversation.
     */
    @GetMapping
    public ResponseEntity<Page<MessageDto>> getMessagesForConversation(
            Authentication authentication,
            @PathVariable String conversationId,
            Pageable pageable) {

        if (!ObjectId.isValid(conversationId)) {
            throw new IllegalArgumentException("Invalid conversation ID format.");
        }

        Page<MessageDto> messages = messageService.getMessagesForConversation(
                new ObjectId(conversationId),
                authentication.getName(),
                pageable
        );

        return ResponseEntity.ok(messages);
    }

    /**
     * Sends a text message to the AI and receives a response.
     * Use Case: Standard text-based chat interaction.
     */
    @PostMapping
    public ResponseEntity<MessageDto> sendMessage(
            Authentication authentication,
            @PathVariable String conversationId,
            @RequestBody CreateMessageRequest request) {

        if (!ObjectId.isValid(conversationId)) {
            throw new IllegalArgumentException("Invalid conversation ID format.");
        }

        MessageDto response = messageService.sendMessage(
                new ObjectId(conversationId),
                authentication.getName(),
                request.getContent(),
                null // No photos for text-only messages
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Sends a message with photos to the AI and receives a response.
     * Use Case: Image-based chat interaction with optional text.
     */
    @PostMapping("/with-photos")
    public ResponseEntity<MessageDto> sendMessageWithPhotos(
            Authentication authentication,
            @PathVariable String conversationId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam("photos") List<MultipartFile> photos) {

        if (!ObjectId.isValid(conversationId)) {
            throw new IllegalArgumentException("Invalid conversation ID format.");
        }

        if (photos == null || photos.isEmpty()) {
            throw new IllegalArgumentException("At least one photo is required.");
        }

        MessageDto response = messageService.sendMessage(
                new ObjectId(conversationId),
                authentication.getName(),
                content,
                photos
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a specific message by ID.
     * Use Case: Getting details of a particular message.
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<MessageDto> getMessage(
            Authentication authentication,
            @PathVariable String conversationId,
            @PathVariable String messageId) {

        if (!ObjectId.isValid(conversationId) || !ObjectId.isValid(messageId)) {
            throw new IllegalArgumentException("Invalid ID format.");
        }

        MessageDto message = messageService.getMessageById(
                new ObjectId(messageId),
                new ObjectId(conversationId),
                authentication.getName()
        );

        return ResponseEntity.ok(message);
    }

    /**
     * Deletes a specific message.
     * Use Case: Allowing users to remove messages from their conversation history.
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            Authentication authentication,
            @PathVariable String conversationId,
            @PathVariable String messageId) {

        if (!ObjectId.isValid(conversationId) || !ObjectId.isValid(messageId)) {
            throw new IllegalArgumentException("Invalid ID format.");
        }

        messageService.deleteMessage(
                new ObjectId(messageId),
                new ObjectId(conversationId),
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * Gets the conversation summary for context.
     * Use Case: Providing conversation context to the AI.
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getConversationSummary(
            Authentication authentication,
            @PathVariable String conversationId) {

        if (!ObjectId.isValid(conversationId)) {
            throw new IllegalArgumentException("Invalid conversation ID format.");
        }

        Map<String, Object> summary = messageService.getConversationSummary(
                new ObjectId(conversationId),
                authentication.getName()
        );

        return ResponseEntity.ok(summary);
    }
}