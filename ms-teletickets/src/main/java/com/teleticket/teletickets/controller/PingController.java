package com.teleticket.teletickets.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint temporal de la fase 0.
 *
 * Confirma que el servicio esta encendido y dice quien es. Es util mientras se
 * levantan 9 servicios a la vez, porque permite saber de inmediato a cual se le
 * esta pegando: al puerto directo o a traves del API Gateway.
 *
 * Se puede eliminar en la fase 16, cuando ya exista la coleccion de Postman.
 */
@RestController
public class PingController {

	@Value("${spring.application.name}")
	private String servicio;

	@Value("${server.port}")
	private int puerto;

	@GetMapping("/ping")
	public Map<String, Object> ping() {
		return Map.of(
				"servicio", servicio,
				"puerto", puerto,
				"estado", "ok",
				"hora", Instant.now().toString());
	}

}
