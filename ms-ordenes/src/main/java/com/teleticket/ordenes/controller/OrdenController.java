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
import com.teleticket.ordenes.exception.NoAutenticadoException;
import com.teleticket.ordenes.service.OrdenService;

import jakarta.validation.Valid;

/**
 * API publica de las ordenes.
 *
 * Se llega a traves del API Gateway (http://localhost:8090/api/ordenes), que reenvia la
 * ruta completa: por eso la base es /api/ordenes.
 *
 * La identidad es la cabecera X-Usuario-Id (seccion 4.3 del diseno); sin ella, 401.
 *
 * Caso de uso: CU-05 (crear la orden y reservar). El pago (CU-06) llega en la fase 9.
 */
@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

	private final OrdenService servicio;

	public OrdenController(OrdenService servicio) {
		this.servicio = servicio;
	}

	/**
	 * Crea una orden con el pase de compra de la cabecera X-Pase-Compra.
	 *
	 * 201 con la orden RESERVADA. 400 sin pase (el gateway ya lo rechaza); 409 si el pase no
	 * sirve, no hay stock, se excede el maximo o la venta esta cerrada; 503 si un servicio
	 * del que depende no responde (en ese caso el pase se devuelve).
	 */
	@PostMapping
	public ResponseEntity<OrdenResponse> crear(
			@RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId,
			@RequestHeader(value = "X-Pase-Compra", required = false) String pase,
			@Valid @RequestBody CrearOrdenRequest req) {
		OrdenResponse orden = servicio.crear(exigirUsuario(usuarioId), pase, req);
		return ResponseEntity.created(URI.create("/api/ordenes/" + orden.id())).body(orden);
	}

	/**
	 * Mis ordenes, la mas reciente primero.
	 *
	 * Esta ruta se declara aparte de /{id}: Spring prefiere la ruta literal, asi que
	 * "mias" nunca se interpreta como un id.
	 */
	@GetMapping("/mias")
	public List<OrdenResponse> mias(@RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId) {
		return servicio.mias(exigirUsuario(usuarioId));
	}

	/** Una orden mia. Si es de otra persona responde 404, igual que si no existiera. */
	@GetMapping("/{ordenId}")
	public OrdenResponse ver(@PathVariable UUID ordenId,
			@RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId) {
		return servicio.consultar(ordenId, exigirUsuario(usuarioId));
	}

	/** Falta o no es un UUID -> 401. */
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
