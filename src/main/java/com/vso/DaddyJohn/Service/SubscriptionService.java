package com.vso.DaddyJohn.Service;

import com.vso.DaddyJohn.Entity.SubscriptionPlan;
import com.vso.DaddyJohn.Entity.UserSubscription;
import com.vso.DaddyJohn.Entity.Users;
import com.vso.DaddyJohn.Repositry.SubscriptionPlanRepo;
import com.vso.DaddyJohn.Repositry.UserSubscriptionRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@AllArgsConstructor
public class SubscriptionService {

    private final UserSubscriptionRepo userSubscriptionRepo;
    private final SubscriptionPlanRepo subscriptionPlanRepo;

    /**
     * Retrieves the currently active subscription plan for a given user.
     * If the user has no active subscription, it defaults to the 'Free' plan.
     *
     * @param user The user whose active plan is to be fetched.
     * @return The active SubscriptionPlan.
     */
    public SubscriptionPlan getActivePlanForUser(Users user) {
        // Find the active subscription record for the user.
        Optional<UserSubscription> activeSubscriptionOpt = userSubscriptionRepo
                .findByUserIdAndIsActive(user.getId(), true);

        // If an active subscription exists, fetch its details.
        if (activeSubscriptionOpt.isPresent()) {
            UserSubscription activeSubscription = activeSubscriptionOpt.get();
            // Fetch the plan details using the plan stored in the subscription.
            // If for some reason the plan is deleted, fall back to the default free plan.
            return subscriptionPlanRepo.findById(activeSubscription.getPlan().getId())
                    .orElseGet(this::getDefaultFreePlan);
        } else {
            // If no active subscription is found, assign the default free plan.
            return getDefaultFreePlan();
        }
    }

    /**
     * Subscribes a user to a new plan. This deactivates any existing plan.
     *
     * @param user The user to subscribe.
     * @param newPlan The new plan to subscribe the user to.
     * @return The newly created UserSubscription record.
     */
    @Transactional
    public UserSubscription subscribeUserToPlan(Users user, SubscriptionPlan newPlan) {
        // Deactivate any currently active subscription for this user.
        userSubscriptionRepo.findByUserIdAndIsActive(user.getId(), true).ifPresent(sub -> {
            sub.setActive(false);
            sub.setEndDate(LocalDate.now());
            userSubscriptionRepo.save(sub);
        });

        // Create the new subscription record.
        UserSubscription newSubscription = new UserSubscription();
        newSubscription.setUser(user);
        newSubscription.setPlan(newPlan);
        newSubscription.setStartDate(LocalDate.now());
        // Set an end date, for example, 30 days from now for a monthly plan.
        newSubscription.setEndDate(LocalDate.now().plusDays(30));
        newSubscription.setActive(true);
        newSubscription.setPaymentStatus("Paid"); // This could also be 'Trial', etc.

        return userSubscriptionRepo.save(newSubscription);
    }

    /**
     * Fetches the default 'Free' plan from the database.
     * Throws an exception if the plan is not configured in the database.
     *
     * @return The default 'Free' SubscriptionPlan.
     */
    private SubscriptionPlan getDefaultFreePlan() {
        return subscriptionPlanRepo.findByName("Free")
                .orElseThrow(() -> new IllegalStateException("FATAL: Default 'Free' plan not found in the database!"));
    }
}