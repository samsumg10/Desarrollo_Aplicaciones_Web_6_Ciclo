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

/**
 * Activa Feign y registra lo que comparten los tres clientes.
 *
 * Los beans declarados aqui los usan todos los clientes de la aplicacion.
 */
@Configuration
@EnableFeignClients(basePackages = "com.teleticket.ordenes.client")
public class FeignConfig {

	/** Convierte las respuestas de error de los otros servicios en excepciones propias. */
	@Bean
	ErrorDecoder errorDecoder(JsonMapper json) {
		return new ClienteErrorDecoder(json);
	}

	/**
	 * Reenvia el X-Request-Id de la peticion que se esta atendiendo a cada llamada Feign.
	 *
	 * Asi la misma compra lleva el mismo identificador en ms-ordenes, ms-cola, ms-eventos y
	 * ms-inventario, y se puede seguir completa en los logs. Funciona porque Feign se ejecuta
	 * en el mismo hilo que atiende la peticion, donde Spring tiene guardada la peticion actual.
	 */
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
