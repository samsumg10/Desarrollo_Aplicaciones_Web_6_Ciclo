package com.teleticket.teletickets.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.teleticket.teletickets.client.dto.CondicionesResponse;

/**
 * Llamadas a ms-eventos (API interna, puerto directo 8082).
 */
@FeignClient(name = "ms-eventos", url = "${servicios.eventos.url}")
public interface EventoClient {

	/**
	 * Las condiciones de ingreso del evento, para copiarlas en cada entrada (PRB-06).
	 *
	 * Aqui no hay una peticion HTTP de entrada de la que heredar el X-Request-Id (se llega por un mensaje),
	 * asi que se pasa el correlationId del mensaje.
	 */
	@GetMapping("/internal/eventos/{eventoId}/condiciones")
	CondicionesResponse condiciones(@PathVariable("eventoId") UUID eventoId,
			@RequestHeader("X-Request-Id") String requestId);
}
