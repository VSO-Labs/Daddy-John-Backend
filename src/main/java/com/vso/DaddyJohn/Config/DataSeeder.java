package com.vso.DaddyJohn.Config;

import com.vso.DaddyJohn.Entity.SubscriptionPlan;
import com.vso.DaddyJohn.Repositry.SubscriptionPlanRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {

    private final SubscriptionPlanRepo subscriptionPlanRepo;

    public DataSeeder(SubscriptionPlanRepo subscriptionPlanRepo) {
        this.subscriptionPlanRepo = subscriptionPlanRepo;
    }

    @Override
    public void run(String... args) throws Exception {
        // Seed the database with a default "Free" plan if it doesn't exist.
        if (subscriptionPlanRepo.findByName("Free").isEmpty()) {
            SubscriptionPlan freePlan = new SubscriptionPlan();
            freePlan.setName("Free");
            freePlan.setDescription("The default free plan for all new users.");
            freePlan.setPricePerMonth(BigDecimal.ZERO);
            freePlan.setMessageLimitPerDay(100); // Example limit for the free plan
            freePlan.setMessageLimitPerHours(20); // Example hourly limit
            subscriptionPlanRepo.save(freePlan);
            System.out.println("--- 'Free' subscription plan has been created ---");
        }
    }
}