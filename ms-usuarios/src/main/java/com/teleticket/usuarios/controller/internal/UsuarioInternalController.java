package com.teleticket.usuarios.controller.internal;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teleticket.usuarios.dto.ContactoResponse;
import com.teleticket.usuarios.service.UsuarioService;

/** API INTERNA: sin ruta en el gateway. La usara ms-notificaciones por Feign (fase 13). */
@RestController
@RequestMapping("/internal/usuarios")
public class UsuarioInternalController {

	private final UsuarioService servicio;

	public UsuarioInternalController(UsuarioService servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/{id}/contacto")
	public ContactoResponse contacto(@PathVariable UUID id) {
		return servicio.contacto(id);
	}
}
