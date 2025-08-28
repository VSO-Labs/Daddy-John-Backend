package com.vso.DaddyJohn.Entity;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "subscription_plans")
@Data
@EntityScan
public class SubscriptionPlan {
    @Id
    private ObjectId id;

    private String name;

    private String description;

    private BigDecimal pricePerMonth = BigDecimal.ZERO;

    private Integer messageLimitPerDay;

    private Integer messageLimitPerHours;

    private String aiAnalysisLevel;

    private String memoryDepth;

    private boolean expressiveModes = false;

    private String supportLevel;

    private boolean privacyEncryption = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
