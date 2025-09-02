package com.vso.DaddyJohn.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DebugController {

    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);

    private final RestTemplate restTemplate;

    @Value("${services.chatbot.django-url}")
    private String djangoApiUrl;

    public DebugController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Check if the Django API is reachable
     */
    @GetMapping("/check-django")
    public ResponseEntity<Map<String, Object>> checkDjangoConnection() {
        Map<String, Object> response = new HashMap<>();

        try {
            logger.info("Checking Django API at: {}", djangoApiUrl);
            response.put("django_url", djangoApiUrl);

            // Try to make a simple GET request to the Django API
            // You might need to adjust this based on what endpoints your Django API has
            ResponseEntity<String> djangoResponse = restTemplate.getForEntity(
                    djangoApiUrl.replace("/chat", ""), // Try the base URL
                    String.class
            );

            response.put("django_reachable", true);
            response.put("django_status", djangoResponse.getStatusCode().value());
            response.put("message", "Django API is reachable");

        } catch (Exception e) {
            logger.error("Failed to reach Django API: ", e);
            response.put("django_reachable", false);
            response.put("error", e.getMessage());
            response.put("message", "Failed to reach Django API");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get current configuration values
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("django_api_url", djangoApiUrl);
        config.put("environment", System.getenv("ENVIRONMENT") != null ? System.getenv("ENVIRONMENT") : "not_set");
        return ResponseEntity.ok(config);
    }

    /**
     * Test the Django chat endpoint with a simple message
     */
    @PostMapping("/test-chat")
    public ResponseEntity<Map<String, Object>> testChatEndpoint(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String testMessage = request.getOrDefault("message", "Hello, this is a test message");
            logger.info("Testing Django chat API at: {} with message: {}", djangoApiUrl, testMessage);

            Map<String, Object> chatRequest = new HashMap<>();
            chatRequest.put("user_input", testMessage);
            chatRequest.put("history", new Object[0]);
            chatRequest.put("latest_summary", null);

            ResponseEntity<Map> djangoResponse = restTemplate.postForEntity(
                    djangoApiUrl,
                    chatRequest,
                    Map.class
            );

            response.put("success", true);
            response.put("django_response", djangoResponse.getBody());
            response.put("status_code", djangoResponse.getStatusCode().value());

        } catch (Exception e) {
            logger.error("Failed to call Django chat API: ", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("error_type", e.getClass().getSimpleName());
        }

        return ResponseEntity.ok(response);
    }
}