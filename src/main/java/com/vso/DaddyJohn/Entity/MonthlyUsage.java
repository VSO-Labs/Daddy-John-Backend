package com.vso.DaddyJohn.Entity;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.YearMonth;
import java.time.LocalDateTime;

@Document(collection = "monthly_usage")
@Data
@CompoundIndex(name = "user_month_unique", def = "{'user': 1, 'usageMonth': 1}", unique = true)
public class MonthlyUsage {
    @Id
    private ObjectId id;

    @DBRef
    private Users user;

    private YearMonth usageMonth; // e.g., 2024-12

    private int tokensUsed = 0;

    private int messagesSent = 0;

    private String currentPlan = "FREE"; // Track the plan at time of usage

    private LocalDateTime lastResetAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}