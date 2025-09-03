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

        // Set the connection timeout (how long to wait to connect to the server).
        requestFactory.setConnectTimeout(30000); // 30 seconds

        // Set the read timeout (how long to wait for a response after connecting).
        requestFactory.setReadTimeout(90000); // 90 seconds (1.5 min)

        // Build the RestTemplate using the custom request factory.
        return builder
                .requestFactory(() -> requestFactory)
                .build();
    }
}
