package com.vso.DaddyJohn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class DaddyJohnApplication {

	public static void main(String[] args) {
		SpringApplication.run(DaddyJohnApplication.class, args);
	}

}
