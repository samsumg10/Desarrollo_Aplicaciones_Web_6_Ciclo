package com.teleticket.usuarios.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teleticket.usuarios.dto.RegistroRequest;
import com.teleticket.usuarios.dto.UsuarioResponse;
import com.teleticket.usuarios.exception.NoAutenticadoException;
import com.teleticket.usuarios.service.UsuarioService;

import jakarta.validation.Valid;

/**
 * API publica (CU-01). Ruta base /api/usuarios: el gateway reenvia la ruta completa.
 *
 * Mientras no exista Spring Security, la identidad llega en la cabecera X-Usuario-Id
 * y los servicios confian en ella (seccion 4.3 del diseno). Cuando llegue el JWT, el
 * gateway escribira esa misma cabecera y este controlador no cambiara.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

	private final UsuarioService servicio;

	public UsuarioController(UsuarioService servicio) {
		this.servicio = servicio;
	}

	@PostMapping
	public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest req) {
		UsuarioResponse creado = servicio.registrar(req);
		return ResponseEntity.created(URI.create("/api/usuarios/" + creado.id())).body(creado);
	}

	/** Datos del usuario identificado por X-Usuario-Id. Sin cabecera -> 401. */
	@GetMapping("/me")
	public UsuarioResponse yo(@RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId) {
		if (usuarioId == null || usuarioId.isBlank()) {
			throw new NoAutenticadoException("Falta la cabecera X-Usuario-Id");
		}
		try {
			return servicio.perfil(UUID.fromString(usuarioId.trim()));
		} catch (IllegalArgumentException e) {
			throw new NoAutenticadoException("X-Usuario-Id no es un UUID valido");
		}
	}
}
