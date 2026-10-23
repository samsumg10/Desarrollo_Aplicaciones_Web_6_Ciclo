package com.teleticket.teletickets.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.teleticket.teletickets.client.dto.CondicionesResponse;

@FeignClient(name = "ms-eventos", url = "${servicios.eventos.url}")
public interface EventoClient {

	@GetMapping("/internal/eventos/{eventoId}/condiciones")
	CondicionesResponse condiciones(@PathVariable("eventoId") UUID eventoId,
			@RequestHeader("X-Request-Id") String requestId);
}
