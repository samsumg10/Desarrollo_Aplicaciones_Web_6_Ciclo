package com.teleticket.cola.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teleticket.cola.dto.TurnoResponse;
import com.teleticket.cola.exception.NoAutenticadoException;
import com.teleticket.cola.service.IngresoService;
import com.teleticket.cola.service.TurnoService;

/**
 * API publica de la sala de espera.
 *
 * Se llega a traves del API Gateway (http://localhost:8090/api/cola/...), que
 * reenvia la ruta completa: por eso la base es /api/cola.
 *
 * La identidad es la cabecera X-Usuario-Id (seccion 4.3 del diseno). Sin ella
 * responde 401 (RN-04): sin sesion no hay turno. Cuando llegue el JWT, el
 * gateway escribira esa misma cabecera y este controlador no cambiara.
 *
 * Casos de uso: CU-03 (entrar a la sala), CU-04 (esperar y recibir el pase).
 */
@RestController
@RequestMapping("/api/cola")
public class ColaController {

	private final IngresoService ingreso;
	private final TurnoService turnos;

	public ColaController(IngresoService ingreso, TurnoService turnos) {
		this.ingreso = ingreso;
		this.turnos = turnos;
	}

	/**
	 * Entra a la sala de espera del evento.
	 *
	 * Idempotente: 201 la primera vez; si ya tenia turno, 200 con el mismo turno.
	 */
	@PostMapping("/eventos/{eventoId}/ingresar")
	public ResponseEntity<TurnoResponse> ingresar(@PathVariable UUID eventoId,
			@RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId) {
		IngresoService.Ingreso resultado = ingreso.ingresar(eventoId, exigirUsuario(usuarioId));
		TurnoResponse turno = resultado.turno();
		if (!resultado.creado()) {
			return ResponseEntity.ok(turno);
		}
		return ResponseEntity.created(URI.create("/api/cola/turnos/" + turno.turnoId())).body(turno);
	}

	/** Mi turno: posicion, espera estimada y, cuando llega, mi pase. Solo lo ve su duenio. */
	@GetMapping("/turnos/{turnoId}")
	public TurnoResponse verTurno(@PathVariable UUID turnoId,
			@RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId) {
		return turnos.consultar(turnoId, exigirUsuario(usuarioId));
	}

	/** Falta o no es un UUID -> 401 (RN-04). */
	private UUID exigirUsuario(String cabecera) {
		if (cabecera == null || cabecera.isBlank()) {
			throw new NoAutenticadoException("Falta la cabecera X-Usuario-Id: inicia sesion para entrar a la sala de espera");
		}
		try {
			return UUID.fromString(cabecera.trim());
		} catch (IllegalArgumentException e) {
			throw new NoAutenticadoException("X-Usuario-Id no es un UUID valido");
		}
	}
}
