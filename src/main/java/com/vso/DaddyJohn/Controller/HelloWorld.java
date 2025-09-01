package com.vso.DaddyJohn.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Controller
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
public class HelloWorld {

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of(
                "status", "UP",
                "message", "Daddy John Backend is running",
                "version", "1.0.0"
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "timestamp", java.time.Instant.now().toString()
        );
    }
}
