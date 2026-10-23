package com.teleticket.teletickets.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.teleticket.teletickets.client")
public class FeignConfig {
}
