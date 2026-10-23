package com.teleticket.ordenes.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.teleticket.ordenes.client.dto.ZonaInternaResponse;

@FeignClient(name = "ms-eventos", url = "${servicios.eventos.url}")
public interface EventoClient {

	@GetMapping("/internal/eventos/{eventoId}/zonas/{zonaId}")
	ZonaInternaResponse datosDeZona(@PathVariable("eventoId") UUID eventoId, @PathVariable("zonaId") UUID zonaId);
}
