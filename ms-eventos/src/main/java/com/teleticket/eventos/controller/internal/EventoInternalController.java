package com.teleticket.eventos.controller.internal;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teleticket.eventos.dto.CondicionesResponse;
import com.teleticket.eventos.dto.ZonaInternaResponse;
import com.teleticket.eventos.service.EventoService;

@RestController
@RequestMapping("/internal/eventos")
public class EventoInternalController {

	private final EventoService servicio;

	public EventoInternalController(EventoService servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/{eventoId}/zonas/{zonaId}")
	public ZonaInternaResponse datosDeZona(
			@PathVariable UUID eventoId,
			@PathVariable UUID zonaId) {
		return servicio.datosDeZona(eventoId, zonaId);
	}

	@GetMapping("/{eventoId}/condiciones")
	public CondicionesResponse condiciones(@PathVariable UUID eventoId) {
		return servicio.condicionesDeIngreso(eventoId);
	}
}
