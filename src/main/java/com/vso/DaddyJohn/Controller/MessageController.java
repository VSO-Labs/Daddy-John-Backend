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

    @PostMapping("/with-photos")
    public ResponseEntity<MessageDto> postNewMessageWithPhotos(
            Authentication authentication,
            @PathVariable String conversationId,
            @RequestParam(value = "message", required = false, defaultValue = "") String content,
            @RequestParam("photos") List<MultipartFile> photos) {
        if (!ObjectId.isValid(conversationId)) {
            throw new IllegalArgumentException("Invalid Conversation ID format. Please provide a valid 24-character hex string.");
        }

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

    @PostMapping("/send")
    public ResponseEntity<MessageDto> sendMessage(
            Authentication authentication,
            @PathVariable String conversationId,
            @RequestParam(value = "message", required = false, defaultValue = "") String content,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos) {
        if (!ObjectId.isValid(conversationId)) {
            throw new IllegalArgumentException("Invalid Conversation ID format. Please provide a valid 24-character hex string.");
        }

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