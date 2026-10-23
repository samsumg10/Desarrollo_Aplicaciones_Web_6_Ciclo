package com.teleticket.eventos.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.teleticket.eventos.dto.CrearEventoRequest;
import com.teleticket.eventos.dto.CrearZonaRequest;
import com.teleticket.eventos.dto.EventoResponse;
import com.teleticket.eventos.dto.ZonaResponse;
import com.teleticket.eventos.service.EventoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

	private final EventoService servicio;

	public EventoController(EventoService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	public List<EventoResponse> listar(
			@RequestParam(defaultValue = "false") boolean incluirBorradores) {
		return incluirBorradores ? servicio.listarTodos() : servicio.listarPublicados();
	}

	@GetMapping("/{eventoId}")
	public EventoResponse verDetalle(@PathVariable UUID eventoId) {
		return servicio.verDetalle(eventoId);
	}

	@PostMapping
	public ResponseEntity<EventoResponse> crear(@Valid @RequestBody CrearEventoRequest req) {
		EventoResponse creado = servicio.crear(req);
		return ResponseEntity
				.created(URI.create("/api/eventos/" + creado.id()))
				.body(creado);
	}

	@PostMapping("/{eventoId}/zonas")
	public ResponseEntity<ZonaResponse> agregarZona(
			@PathVariable UUID eventoId,
			@Valid @RequestBody CrearZonaRequest req) {
		ZonaResponse zona = servicio.agregarZona(eventoId, req);
		return ResponseEntity
				.created(URI.create("/api/eventos/" + eventoId + "/zonas/" + zona.id()))
				.body(zona);
	}

	@PutMapping("/{eventoId}")
	public EventoResponse actualizar(
			@PathVariable UUID eventoId,
			@Valid @RequestBody CrearEventoRequest req) {
		return servicio.actualizar(eventoId, req);
	}

	@PutMapping("/{eventoId}/publicar")
	public EventoResponse publicar(@PathVariable UUID eventoId,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		return servicio.publicar(eventoId, requestId);
	}
}
