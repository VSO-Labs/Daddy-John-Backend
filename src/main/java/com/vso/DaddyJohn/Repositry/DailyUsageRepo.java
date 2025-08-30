package com.vso.DaddyJohn.Repositry;

import com.vso.DaddyJohn.Entity.DailyUsage;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyUsageRepo extends MongoRepository<DailyUsage, ObjectId> {
    // Find today's usage record for a specific user
    Optional<DailyUsage> findByUser_IdAndUsageDate(ObjectId userId, LocalDate date);
}
