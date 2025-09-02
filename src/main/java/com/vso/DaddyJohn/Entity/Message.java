package com.vso.DaddyJohn.Entity;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "messages")
@Data
public class Message {
    public void setConversationId(ObjectId conversationId) {
        // This method can be used for setting conversation ID directly if needed
        // But it's better to use the conversation object reference
    }

    public enum Role {
        USER, ASSISTANT, SYSTEM
    }

    @Id
    private ObjectId id;

    @DBRef
    private Conversation conversation;

    private Role role;

    private String content;

    private Integer tokenCount;

    // New field to store photo URLs
    private List<String> photoUrls;

    // New field to indicate message type
    private MessageType messageType = MessageType.TEXT;

    // New field for file metadata (if needed)
    private List<FileMetadata> attachments;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum MessageType {
        TEXT,
        IMAGE,
        TEXT_WITH_IMAGE
    }

    @Data
    public static class FileMetadata {
        private String originalName;
        private String storedName;
        private String fileType;
        private long fileSize;
        private String url;
    }
}