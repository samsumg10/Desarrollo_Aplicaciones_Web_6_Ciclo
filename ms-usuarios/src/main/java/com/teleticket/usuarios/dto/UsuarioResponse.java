package com.teleticket.usuarios.dto;

import java.time.Instant;
import java.util.UUID;

import com.teleticket.usuarios.entity.Usuario;

public record UsuarioResponse(UUID id, String dni, String nombres, String email,
		String rol, Instant creadoEn) {

	public static UsuarioResponse de(Usuario u) {
		return new UsuarioResponse(u.getId(), u.getDni(), u.getNombres(), u.getEmail(),
				u.getRol().name(), u.getCreadoEn());
	}
}
