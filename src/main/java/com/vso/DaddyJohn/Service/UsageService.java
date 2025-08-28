package com.vso.DaddyJohn.Service;

import com.vso.DaddyJohn.Entity.DailyUsage;
import com.vso.DaddyJohn.Entity.SubscriptionPlan;
import com.vso.DaddyJohn.Entity.Users;
import com.vso.DaddyJohn.Repositry.DailyUsageRepo;
import lombok.AllArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@AllArgsConstructor
public class UsageService {

    private final DailyUsageRepo dailyUsageRepo;
    private final SubscriptionService subscriptionService;

    /**
     * Checks if a user can send a message based on their active plan's limits.
     * @param user The user.
     * @return true if they are within limits, false otherwise.
     */
    public boolean canSendMessage(Users user) {
        SubscriptionPlan plan = subscriptionService.getActivePlanForUser(user);
        DailyUsage usage = getTodaysUsage(user.getId());

        // Check against the plan's message limit
        // A limit of -1 can mean "unlimited"
        if (plan.getMessageLimitPerDay() != -1 && usage.getMessagesSent() >= plan.getMessageLimitPerDay()) {
            return false;
        }

        return true;
    }

    /**
     * Records the message and token usage for a user.
     * @param user The user.
     * @param tokenCount The number of tokens consumed.
     */
    public void recordUsage(Users user, int tokenCount) {
        DailyUsage usage = getTodaysUsage(user.getId());
        usage.setMessagesSent(usage.getMessagesSent() + 1);
        usage.setTokensUsed(usage.getTokensUsed() + tokenCount);
        dailyUsageRepo.save(usage);
    }

    private DailyUsage getTodaysUsage(ObjectId userId) {
        LocalDate today = LocalDate.now();
        return dailyUsageRepo.findByUserIdAndUsageDate(userId, today)
                .orElseGet(() -> {
                    DailyUsage newUsage = new DailyUsage();
                    newUsage.setUserId(userId);
                    newUsage.setUsageDate(today);
                    return dailyUsageRepo.save(newUsage);
                });
    }
}