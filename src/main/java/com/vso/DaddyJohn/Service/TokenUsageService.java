package com.vso.DaddyJohn.Service;

import com.vso.DaddyJohn.Config.PlanConfiguration;
import com.vso.DaddyJohn.Entity.MonthlyUsage;
import com.vso.DaddyJohn.Entity.SubscriptionPlan;
import com.vso.DaddyJohn.Entity.Users;
import com.vso.DaddyJohn.Repositry.MonthlyUsageRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenUsageService {

    private final MonthlyUsageRepo monthlyUsageRepo;
    private final SubscriptionService subscriptionService;
    private final PlanConfiguration planConfiguration;

    /**
     * Calculate tokens based on input and output length
     */
    public int calculateTokens(String userInput, String brainOutput) {
        int inputLength = userInput != null ? userInput.length() : 0;
        int outputLength = brainOutput != null ? brainOutput.length() : 0;
        return (inputLength + outputLength) / 4;
    }

    /**
     * Check if user can send message based on token limits
     */
    public boolean canSendMessage(Users user, String messageContent) {
        try {
            SubscriptionPlan plan = subscriptionService.getActivePlanForUser(user);

            // If unlimited plan, always allow
            if (plan.isUnlimited()) {
                return true;
            }

            MonthlyUsage usage = getCurrentMonthUsage(user);
            int planLimit = getPlanTokenLimit(plan.getName());

            // Estimate tokens for this message (assume average response)
            int estimatedTokens = calculateTokens(messageContent, messageContent); // Rough estimate

            return (usage.getTokensUsed() + estimatedTokens) <= planLimit;
        } catch (Exception e) {
            log.error("Error checking token limit for user {}: {}", user.getUsername(), e.getMessage());
            return false; // Fail safe
        }
    }

    /**
     * Record token usage after message is processed
     */
    @Transactional
    public void recordTokenUsage(Users user, int tokensUsed) {
        try {
            MonthlyUsage usage = getCurrentMonthUsage(user);
            SubscriptionPlan plan = subscriptionService.getActivePlanForUser(user);

            usage.setTokensUsed(usage.getTokensUsed() + tokensUsed);
            usage.setMessagesSent(usage.getMessagesSent() + 1);
            usage.setCurrentPlan(plan.getName());
            usage.setUpdatedAt(LocalDateTime.now());

            monthlyUsageRepo.save(usage);
            log.debug("Recorded {} tokens for user {}", tokensUsed, user.getUsername());
        } catch (Exception e) {
            log.error("Error recording token usage: {}", e.getMessage());
        }
    }

    /**
     * Get current month's usage or create new record
     */
    public MonthlyUsage getCurrentMonthUsage(Users user) {
        YearMonth currentMonth = YearMonth.now();
        return monthlyUsageRepo.findByUserAndUsageMonth(user, currentMonth)
                .orElseGet(() -> {
                    MonthlyUsage newUsage = new MonthlyUsage();
                    newUsage.setUser(user);
                    newUsage.setUsageMonth(currentMonth);
                    newUsage.setCurrentPlan(subscriptionService.getActivePlanForUser(user).getName());
                    return monthlyUsageRepo.save(newUsage);
                });
    }

    /**
     * Get remaining tokens for user
     */
    public Map<String, Object> getUserTokenStatus(Users user) {
        SubscriptionPlan plan = subscriptionService.getActivePlanForUser(user);
        MonthlyUsage usage = getCurrentMonthUsage(user);
        int planLimit = getPlanTokenLimit(plan.getName());

        Map<String, Object> status = new HashMap<>();
        status.put("plan", plan.getName());
        status.put("tokensUsed", usage.getTokensUsed());
        status.put("tokensLimit", planLimit == -1 ? "UNLIMITED" : planLimit);
        status.put("tokensRemaining", planLimit == -1 ? "UNLIMITED" : (planLimit - usage.getTokensUsed()));
        status.put("messagesSent", usage.getMessagesSent());

        return status;
    }

    /**
     * Get token limit for a plan from configuration
     */
    private int getPlanTokenLimit(String planName) {
        PlanConfiguration.PlanLimits limits = planConfiguration.getPlanLimits(planName);
        return limits != null ? limits.getTokens() : 500; // Default to free tier
    }

    /**
     * Reset monthly usage - runs on the 1st of each month at midnight
     */
    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void resetMonthlyUsage() {
        log.info("Starting monthly usage reset...");
        YearMonth lastMonth = YearMonth.now().minusMonths(1);

        // Archive or update last month's records
        monthlyUsageRepo.findByUsageMonth(lastMonth).forEach(usage -> {
            usage.setLastResetAt(LocalDateTime.now());
            monthlyUsageRepo.save(usage);
        });

        log.info("Monthly usage reset completed");
    }
}