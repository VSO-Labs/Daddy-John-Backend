package com.vso.DaddyJohn.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.plans")
public class PlanConfiguration {

    @NotNull
    private Map<String, PlanLimits> limits;

    @Data
    public static class PlanLimits {
        private int tokens; // -1 for unlimited
        private String displayName;
        private String description;
    }

    public PlanLimits getPlanLimits(String planName) {
        return limits.getOrDefault(planName.toLowerCase(),
                limits.getOrDefault("free", getDefaultFreePlan()));
    }

    private PlanLimits getDefaultFreePlan() {
        PlanLimits freePlan = new PlanLimits();
        freePlan.setTokens(500);
        freePlan.setDisplayName("Free");
        freePlan.setDescription("Free tier with limited tokens");
        return freePlan;
    }
}