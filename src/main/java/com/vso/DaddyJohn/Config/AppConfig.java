package com.vso.DaddyJohn.Config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Create a request factory that allows you to configure timeouts.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        // Increase connection timeout for production environment
        requestFactory.setConnectTimeout(60000); // 60 seconds (increased from 30)

        // Increase read timeout for AI responses which can take time
        requestFactory.setReadTimeout(120000); // 120 seconds (2 minutes, increased from 90)

        // Build the RestTemplate using the custom request factory.
        return builder
                .requestFactory(() -> requestFactory)
                .build();
    }
}