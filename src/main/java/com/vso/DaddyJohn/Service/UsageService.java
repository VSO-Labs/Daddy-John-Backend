package com.vso.DaddyJohn.Service;

import com.vso.DaddyJohn.Entity.DailyUsage;
import com.vso.DaddyJohn.Entity.SubscriptionPlan;
import com.vso.DaddyJohn.Entity.Users;
import com.vso.DaddyJohn.Repositry.DailyUsageRepo;
import com.vso.DaddyJohn.Repositry.UserRepo;
import lombok.AllArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
@AllArgsConstructor
public class UsageService {

    private final DailyUsageRepo dailyUsageRepo;
    private final SubscriptionService subscriptionService;
    private final UserRepo userRepo;

    public boolean canSendMessage(Users user) {
        SubscriptionPlan plan = subscriptionService.getActivePlanForUser(user);
        DailyUsage usage = getTodaysUsage(user.getId());

        // Check against the plan's message limit
        if (plan.getMessageLimitPerDay() != null && plan.getMessageLimitPerDay() >= 0 && usage.getMessagesSent() >= plan.getMessageLimitPerDay()) {
            return false;
        }
        return true;
    }

    public void recordUsage(Users user, int tokenCount) {
        DailyUsage usage = getTodaysUsage(user.getId());
        usage.setMessagesSent(usage.getMessagesSent() + 1);
        usage.setTokensUsed(usage.getTokensUsed() + tokenCount);
        dailyUsageRepo.save(usage);
    }

    private DailyUsage getTodaysUsage(ObjectId userId) {
        LocalDate today = LocalDate.now();
        return dailyUsageRepo.findByUser_IdAndUsageDate(userId, today)
                .orElseGet(() -> {
                    DailyUsage newUsage = new DailyUsage();
                    userRepo.findById(userId).ifPresent(newUsage::setUser);
                    newUsage.setUsageDate(today);
                    return dailyUsageRepo.save(newUsage);
                });
    }
}