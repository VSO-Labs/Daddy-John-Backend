package com.vso.DaddyJohn.Repositry;

import com.vso.DaddyJohn.Entity.MonthlyUsage;
import com.vso.DaddyJohn.Entity.Users;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyUsageRepo extends MongoRepository<MonthlyUsage, ObjectId> {

    Optional<MonthlyUsage> findByUserAndUsageMonth(Users user, YearMonth month);

    List<MonthlyUsage> findByUsageMonth(YearMonth month);

    Optional<MonthlyUsage> findByUser_IdAndUsageMonth(ObjectId userId, YearMonth month);
}