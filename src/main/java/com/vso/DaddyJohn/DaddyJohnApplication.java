package com.vso.DaddyJohn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DaddyJohnApplication {

	public static void main(String[] args) {
		SpringApplication.run(DaddyJohnApplication.class, args);
	}
}