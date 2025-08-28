package com.vso.DaddyJohn.Repositry;

import com.vso.DaddyJohn.Entity.SubscriptionPlan;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionPlanRepo extends MongoRepository<SubscriptionPlan, ObjectId> {
    // Find a plan by its name (e.g., "Free", "Premium")
    Optional<SubscriptionPlan> findByName(String name);
}