package com.vso.DaddyJohn.Entity;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "user_subscriptions")
@Data
public class UserSubscription {
    @Id
    private ObjectId id;
    @DBRef
    private Users user;
    @DBRef
    private SubscriptionPlan plan;
    private boolean isActive = true;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate renewalDate;
    private String paymentStatus;
    private String paymentGatewayId;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}