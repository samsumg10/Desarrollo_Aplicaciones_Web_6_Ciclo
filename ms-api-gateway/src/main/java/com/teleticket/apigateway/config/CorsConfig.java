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

/**
 * CORS: permite que el front (fase 18) llame al Gateway desde el navegador.
 *
 * Que es CORS, en una frase: por seguridad, el navegador bloquea las llamadas
 * de una pagina a un servidor que esta en otra direccion, salvo que ese
 * servidor diga explicitamente "acepto llamadas desde ahi". Esto es ese
 * permiso.
 *
 * Hoy no se usa, porque todavia no hay front y curl o Postman no aplican
 * CORS (es una regla del navegador, no del protocolo). Se deja listo porque
 * cuando llegue el front, el clasico error de CORS cuesta horas de depurar.
 *
 * Los origenes permitidos salen de application.properties, no estan escritos
 * aqui: asi se cambian sin recompilar.
 */
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

		// Las cabeceras propias del proyecto tienen que estar permitidas,
		// o el navegador bloquea la peticion antes de enviarla.
		config.setAllowedHeaders(List.of("*"));

		// Para que el front pueda LEER estas cabeceras de la respuesta.
		// Sin exposedHeaders, el navegador las oculta aunque lleguen.
		config.setExposedHeaders(List.of("X-Request-Id"));

		config.setAllowCredentials(true);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/api/**", config);

		FilterRegistrationBean<CorsFilter> registro = new FilterRegistrationBean<>(new CorsFilter(fuente));
		// Antes que todo lo demas: una peticion OPTIONS de verificacion previa
		// debe responderse sin pasar por el filtro del pase.
		registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registro;
	}
}
