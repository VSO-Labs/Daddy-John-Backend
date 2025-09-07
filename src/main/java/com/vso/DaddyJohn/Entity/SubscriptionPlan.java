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

    private String name; // FREE, MEDIUM, HIGH, UNLIMITED

    private String description;

    private BigDecimal pricePerMonth = BigDecimal.ZERO;

    // Token limits
    private Integer monthlyTokenLimit;

    @Deprecated
    private Integer messageLimitPerDay;

    @Deprecated
    private Integer messageLimitPerHours;

    private String aiAnalysisLevel;

    private String memoryDepth;

    private boolean expressiveModes = false;

    private String supportLevel;

    private boolean privacyEncryption = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    public boolean isUnlimited() {
        return monthlyTokenLimit != null && monthlyTokenLimit == -1;
    }
}