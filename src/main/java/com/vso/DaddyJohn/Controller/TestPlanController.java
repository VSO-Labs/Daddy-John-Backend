package com.vso.DaddyJohn.Controller;

import com.vso.DaddyJohn.Entity.Users;
import com.vso.DaddyJohn.Repositry.UserRepo;
import com.vso.DaddyJohn.Service.TokenUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestPlanController {

    private final TokenUsageService tokenUsageService;
    private final UserRepo userRepo;

    /**
     * Internal testing endpoint for plan usage
     * Input: { "userId": "...", "message": "..." }
     * Output: { "tokensUsed": 34, "tokensRemaining": 1450, "plan": "MEDIUM" }
     */
    @PostMapping("/plan-usage")
    public ResponseEntity<Map<String, Object>> testPlanUsage(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String message = request.get("message");

        if (userId == null || message == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "userId and message are required"
            ));
        }

        Users user;
        try {
            // Try to find by ObjectId first
            if (ObjectId.isValid(userId)) {
                user = userRepo.findById(new ObjectId(userId))
                        .orElse(null);
            } else {
                // Fall back to username
                user = userRepo.findByUsername(userId);
            }

            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "User not found"
                ));
            }
        } catch (Exception e) {
            log.error("Error finding user: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid user ID"
            ));
        }

        // Simulate a response of similar length
        String simulatedResponse = "This is a simulated response. " + message;
        int tokensForThisMessage = tokenUsageService.calculateTokens(message, simulatedResponse);

        // Get current status
        Map<String, Object> currentStatus = tokenUsageService.getUserTokenStatus(user);

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getId().toHexString());
        response.put("username", user.getUsername());
        response.put("messageLength", message.length());
        response.put("estimatedTokensForMessage", tokensForThisMessage);
        response.put("currentTokensUsed", currentStatus.get("tokensUsed"));
        response.put("tokensRemaining", currentStatus.get("tokensRemaining"));
        response.put("plan", currentStatus.get("plan"));
        response.put("tokensLimit", currentStatus.get("tokensLimit"));
        response.put("canSendMessage", tokenUsageService.canSendMessage(user, message));

        return ResponseEntity.ok(response);
    }

    /**
     * Reset user's monthly usage (for testing)
     */
    @PostMapping("/reset-usage/{userId}")
    public ResponseEntity<Map<String, Object>> resetUsage(@PathVariable String userId) {
        Users user;
        try {
            if (ObjectId.isValid(userId)) {
                user = userRepo.findById(new ObjectId(userId))
                        .orElse(null);
            } else {
                user = userRepo.findByUsername(userId);
            }

            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "User not found"
                ));
            }

            // Reset by getting current month usage and setting to 0
            var monthlyUsage = tokenUsageService.getCurrentMonthUsage(user);
            monthlyUsage.setTokensUsed(0);
            monthlyUsage.setMessagesSent(0);

            return ResponseEntity.ok(Map.of(
                    "message", "Usage reset successfully",
                    "userId", user.getId().toHexString(),
                    "username", user.getUsername()
            ));
        } catch (Exception e) {
            log.error("Error resetting usage: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to reset usage"
            ));
        }
    }

    @PostMapping("/me/usage")
    public ResponseEntity<Map<String, Object>> getUserTokenUsage(Authentication authentication) {
        // 1. Find the user entity from the username in the JWT token.
        Users currentUser = userRepo.findByUsername(authentication.getName());

        // 2. Delegate to the TokenUsageService to get the status details.
        Map<String, Object> tokenStatus = tokenUsageService.getUserTokenStatus(currentUser);

        // 3. Return the status map with a 200 OK response.
        return ResponseEntity.ok(tokenStatus);
    }
}