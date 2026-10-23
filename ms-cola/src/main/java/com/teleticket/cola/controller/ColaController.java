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

@RestController
@RequestMapping("/api/cola")
public class ColaController {

	private final IngresoService ingreso;
	private final TurnoService turnos;

	public ColaController(IngresoService ingreso, TurnoService turnos) {
		this.ingreso = ingreso;
		this.turnos = turnos;
	}

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

	@GetMapping("/turnos/{turnoId}")
	public TurnoResponse verTurno(@PathVariable UUID turnoId,
			@RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId) {
		return turnos.consultar(turnoId, exigirUsuario(usuarioId));
	}

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
