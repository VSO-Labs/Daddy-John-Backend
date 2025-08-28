package com.vso.DaddyJohn.Repositry;

import com.vso.DaddyJohn.Entity.ApiAccessLog;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiAccessLogRepo extends MongoRepository<ApiAccessLog, ObjectId> {
    // This repository will be used for logging API calls.
    // We can add custom query methods here later if needed for analytics.
}
