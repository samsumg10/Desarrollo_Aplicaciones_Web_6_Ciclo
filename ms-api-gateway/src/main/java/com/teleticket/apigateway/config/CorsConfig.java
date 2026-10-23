package com.teleticket.apigateway.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;

@Configuration
public class CorsConfig {

	@Value("${teleticket.cors.origenes-permitidos}")
	private String origenesPermitidos;

	@Bean
	public FilterRegistrationBean<CorsFilter> corsFilter() {
		CorsConfiguration config = new CorsConfiguration();

		config.setAllowedOrigins(Arrays.stream(origenesPermitidos.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList());

		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

		config.setAllowedHeaders(List.of("*"));

		config.setExposedHeaders(List.of("X-Request-Id"));

		config.setAllowCredentials(true);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/api/**", config);

		FilterRegistrationBean<CorsFilter> registro = new FilterRegistrationBean<>(new CorsFilter(fuente));
		registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registro;
	}
}
