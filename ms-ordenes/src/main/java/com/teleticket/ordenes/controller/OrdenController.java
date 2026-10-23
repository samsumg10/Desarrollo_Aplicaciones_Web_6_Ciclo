package com.teleticket.ordenes.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teleticket.ordenes.dto.CrearOrdenRequest;
import com.teleticket.ordenes.dto.OrdenResponse;
import com.teleticket.ordenes.dto.PagarRequest;
import com.teleticket.ordenes.exception.NoAutenticadoException;
import com.teleticket.ordenes.service.OrdenService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

	private final OrdenService servicio;

	public OrdenController(OrdenService servicio) {
		this.servicio = servicio;
	}

	@PostMapping
	public ResponseEntity<OrdenResponse> crear(
			@RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId,
			@RequestHeader(value = "X-Pase-Compra", required = false) String pase,
			@Valid @RequestBody CrearOrdenRequest req) {
		OrdenResponse orden = servicio.crear(exigirUsuario(usuarioId), pase, req);
		return ResponseEntity.created(URI.create("/api/ordenes/" + orden.id())).body(orden);
	}

	@PostMapping("/{ordenId}/pagar")
	public OrdenResponse pagar(@PathVariable UUID ordenId,
			@RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId,
			@Valid @RequestBody PagarRequest req) {
		return servicio.pagar(exigirUsuario(usuarioId), ordenId, req, requestId);
	}

	@GetMapping("/mias")
	public List<OrdenResponse> mias(@RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId) {
		return servicio.mias(exigirUsuario(usuarioId));
	}

	@GetMapping("/{ordenId}")
	public OrdenResponse ver(@PathVariable UUID ordenId,
			@RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId) {
		return servicio.consultar(ordenId, exigirUsuario(usuarioId));
	}

	private UUID exigirUsuario(String cabecera) {
		if (cabecera == null || cabecera.isBlank()) {
			throw new NoAutenticadoException("Falta la cabecera X-Usuario-Id");
		}
		try {
			return UUID.fromString(cabecera.trim());
		} catch (IllegalArgumentException e) {
			throw new NoAutenticadoException("X-Usuario-Id no es un UUID valido");
		}
	}
}
