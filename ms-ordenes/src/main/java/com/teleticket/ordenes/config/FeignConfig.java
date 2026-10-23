package com.teleticket.ordenes.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.teleticket.ordenes.client.ClienteErrorDecoder;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableFeignClients(basePackages = "com.teleticket.ordenes.client")
public class FeignConfig {

	@Bean
	ErrorDecoder errorDecoder(JsonMapper json) {
		return new ClienteErrorDecoder(json);
	}

	@Bean
	RequestInterceptor requestIdInterceptor() {
		return plantilla -> {
			if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
				String requestId = atributos.getRequest().getHeader("X-Request-Id");
				if (requestId != null && !requestId.isBlank()) {
					plantilla.header("X-Request-Id", requestId);
				}
			}
		};
	}
}
