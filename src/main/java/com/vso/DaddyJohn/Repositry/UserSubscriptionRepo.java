package com.vso.DaddyJohn.Repositry;

import com.vso.DaddyJohn.Entity.UserSubscription;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepo extends MongoRepository<UserSubscription, ObjectId> {
    // Correctly find an active subscription by the user's ObjectId
    Optional<UserSubscription> findByUserIdAndIsActive(ObjectId userId, boolean isActive);
}