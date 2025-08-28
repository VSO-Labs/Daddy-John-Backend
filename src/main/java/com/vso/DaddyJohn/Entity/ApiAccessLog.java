package com.vso.DaddyJohn.Entity;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "api_access_logs")
@Data
public class ApiAccessLog {
    @Id
    private ObjectId id;

    @DBRef
    private Users user;

    private String endpoint;

    private Object requestPayload; // Using Object for flexibility with JSONB

    private Object responsePayload; // Using Object for flexibility with JSONB

    private int statusCode;

    private int tokensUsed = 0;

    private LocalDateTime createdAt = LocalDateTime.now();
}
