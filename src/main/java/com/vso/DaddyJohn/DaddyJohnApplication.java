package com.vso.DaddyJohn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class DaddyJohnApplication {

    public static void main(String[] args) {
        SpringApplication.run(DaddyJohnApplication.class, args);
    }
}

