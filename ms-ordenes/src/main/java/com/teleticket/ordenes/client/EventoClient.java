package com.teleticket.ordenes.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.teleticket.ordenes.client.dto.ZonaInternaResponse;

/**
 * Llamadas a ms-eventos (API interna, puerto directo 8082).
 */
@FeignClient(name = "ms-eventos", url = "${servicios.eventos.url}")
public interface EventoClient {

	/** Precio, aforo, maximo por comprador y si la venta ya esta abierta. Solo lee. */
	@GetMapping("/internal/eventos/{eventoId}/zonas/{zonaId}")
	ZonaInternaResponse datosDeZona(@PathVariable("eventoId") UUID eventoId, @PathVariable("zonaId") UUID zonaId);
}
