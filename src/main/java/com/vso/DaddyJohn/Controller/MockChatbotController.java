package com.vso.DaddyJohn.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/mock/chat") // A distinct path for the mock API
public class MockChatbotController {

    @PostMapping
    public ResponseEntity<Map<String, Object>> mockChat(@RequestBody Map<String, String> payload) throws InterruptedException {
        String message = payload.get("message");
        String responseText = "This is a mock response to your message: '" + message + "'";
        int tokenCount = (int) Math.ceil(responseText.length() / 4.0);

        // Simulate network delay
        TimeUnit.SECONDS.sleep(1);

        Map<String, Object> response = Map.of(
                "response", responseText,
                "token_count", tokenCount
        );
        return ResponseEntity.ok(response);
    }
}