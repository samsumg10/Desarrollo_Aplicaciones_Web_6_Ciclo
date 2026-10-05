package org.citcuit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class CircuitBreakApplication {

	public static void main(String[] args) {
		SpringApplication.run(CircuitBreakApplication.class, args);
		System.out.println("Circuit Break ...!!!");
	}

}
