package com.teleticket.teletickets.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Activa Feign: ms-tickets solo llama a ms-eventos, para pedir las condiciones de ingreso.
 */
@Configuration
@EnableFeignClients(basePackages = "com.teleticket.teletickets.client")
public class FeignConfig {
}
