package com.vso.DaddyJohn.Entity;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "messages")
@Data
public class Message {
    public void setConversationId(ObjectId conversationId) {
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

    private LocalDateTime createdAt = LocalDateTime.now();
}
