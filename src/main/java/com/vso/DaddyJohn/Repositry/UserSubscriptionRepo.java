package com.vso.DaddyJohn.Repositry;

import com.vso.DaddyJohn.Entity.UserSubscription;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSubscriptionRepo extends MongoRepository<UserSubscription, ObjectId> {
    // Find the active subscription for a user
    Optional<UserSubscription> findByUserIdAndIsActiveTrue(ObjectId userId);
}
