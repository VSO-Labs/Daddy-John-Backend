package com.vso.DaddyJohn.Service;

import com.vso.DaddyJohn.Entity.SubscriptionPlan;
import com.vso.DaddyJohn.Entity.UserSubscription;
import com.vso.DaddyJohn.Entity.Users;
import com.vso.DaddyJohn.Repositry.SubscriptionPlanRepo;
import com.vso.DaddyJohn.Repositry.UserSubscriptionRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@AllArgsConstructor
public class SubscriptionService {

    private final UserSubscriptionRepo userSubscriptionRepo;
    private final SubscriptionPlanRepo subscriptionPlanRepo;


    public SubscriptionPlan getActivePlanForUser(Users user) {
        Optional<UserSubscription> activeSubscription = userSubscriptionRepo
                .findByUserIdAndIsActive(user.getId(), true);

        if (activeSubscription.isPresent()) {
            return subscriptionPlanRepo.findById(activeSubscription.get().getPlanId())
                    .orElseGet(this::getDefaultFreePlan);
        } else {
            return getDefaultFreePlan();
        }
    }


    private SubscriptionPlan getDefaultFreePlan() {
        return subscriptionPlanRepo.findByName("Free")
                .orElseThrow(() -> new IllegalStateException("Default 'Free' plan not found in database!"));
    }

    /**
     * Creates a new subscription for a user. (For future use with payments)
     */
    public UserSubscription subscribeUserToPlan(Users user, SubscriptionPlan plan) {
        // Deactivate any existing subscriptions for the user
        userSubscriptionRepo.findByUserIdAndIsActive(user.getId(), true).ifPresent(sub -> {
            sub.setActive(false);
            sub.setEndDate(LocalDate.now());
            userSubscriptionRepo.save(sub);
        });

        UserSubscription newSubscription = new UserSubscription();
        newSubscription.setUserId(user.getId());
        newSubscription.setPlanId(plan.getId());
        newSubscription.setStartDate(LocalDate.now());
        // End date could be set based on the plan (e.g., 30 days from now)
        newSubscription.setEndDate(LocalDate.now().plusDays(30));
        newSubscription.setActive(true);
        newSubscription.setPaymentStatus("Paid"); // Or "Trial"

        return userSubscriptionRepo.save(newSubscription);
    }
}