package com.vso.DaddyJohn.Controller;

import com.vso.DaddyJohn.Dto.MessageDto;
import com.vso.DaddyJohn.Service.MessageService;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Handles all API requests for messages within a specific conversation.
 * Use Case: Manages the sending and receiving of chat messages with optional photo attachments.
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
     * Retrieves a paginated list of all messages within a given conversation.
     * Use Case: To load and display the chat history when a user opens a conversation.
     */
    @GetMapping
    public Page<MessageDto> getAllMessagesForConversation(
            Authentication authentication,
            @PathVariable String conversationId,
            Pageable pageable) {

        if (!ObjectId.isValid(conversationId)) {
            throw new IllegalArgumentException("Invalid Conversation ID format. Please provide a valid 24-character hex string.");
        }
        return messageService.getAllMessagesForConversation(
                new ObjectId(conversationId),
                authentication.getName(),
                pageable
        );
    }

    /**
     * Posts a new text-only message to a conversation and gets the AI's response.
     * Use Case: When a user types a text message and hits 'send' in the chat window.
     */
    @PostMapping
    public ResponseEntity<MessageDto> postNewTextMessage(
            Authentication authentication,
            @PathVariable String conversationId,
            @RequestBody Map<String, String> body) {

        String content = body.get("message");

        if (!ObjectId.isValid(conversationId)) {
            throw new IllegalArgumentException("Invalid Conversation ID format. Please provide a valid 24-character hex string.");
        }

        MessageDto response = messageService.postNewMessage(
                new ObjectId(conversationId),
                content,
                authentication.getName()
        );

        return ResponseEntity.ok(response);
    }


    /**
     * Posts a new message with photos to a conversation and gets the AI's response.
     * Use Case: When a user uploads photos with or without text and sends them.
     */
    @PostMapping("/with-photos")
    public ResponseEntity<MessageDto> postNewMessageWithPhotos(
            Authentication authentication,
            @PathVariable String conversationId,
            @RequestParam(value = "message", required = false, defaultValue = "") String content,
            @RequestParam("photos") List<MultipartFile> photos) {

        if (!ObjectId.isValid(conversationId)) {
            throw new IllegalArgumentException("Invalid Conversation ID format. Please provide a valid 24-character hex string.");
        }

        // Validate that at least photos or message content is provided
        if ((content == null || content.trim().isEmpty()) && (photos == null || photos.isEmpty())) {
            throw new IllegalArgumentException("Either message content or photos must be provided.");
        }

        MessageDto response = messageService.postNewMessage(
                new ObjectId(conversationId),
                content.trim().isEmpty() ? "Photo message" : content,
                authentication.getName(),
                photos
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Alternative endpoint that handles both text and photos in a single request
     * Use Case: Unified endpoint for frontend to send any type of message
     */
    @PostMapping("/send")
    public ResponseEntity<MessageDto> sendMessage(
            Authentication authentication,
            @PathVariable String conversationId,
            @RequestParam(value = "message", required = false, defaultValue = "") String content,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos) {

        if (!ObjectId.isValid(conversationId)) {
            throw new IllegalArgumentException("Invalid Conversation ID format. Please provide a valid 24-character hex string.");
        }

        // Validate that at least photos or message content is provided
        if ((content == null || content.trim().isEmpty()) && (photos == null || photos.isEmpty())) {
            throw new IllegalArgumentException("Either message content or photos must be provided.");
        }

        MessageDto response = messageService.postNewMessage(
                new ObjectId(conversationId),
                content.trim().isEmpty() ? "Photo message" : content,
                authentication.getName(),
                photos
        );

        return ResponseEntity.ok(response);
    }
}