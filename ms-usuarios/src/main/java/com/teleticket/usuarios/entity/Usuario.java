package com.teleticket.usuarios.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Un comprador o miembro del personal (CU-01).
 *
 * Guarda el HASH de la contrasena, nunca la contrasena. No tiene setters:
 * un usuario creado no se modifica desde fuera.
 */
@Entity
@Table(name = "usuario")
@Getter
@NoArgsConstructor
public class Usuario {

	@Id
	private UUID id;

	@Column(nullable = false, length = 8)
	private String dni;

	@Column(nullable = false, length = 150)
	private String nombres;

	@Column(nullable = false, length = 150)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Rol rol;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	public static Usuario crear(UUID id, String dni, String nombres, String email,
			String passwordHash, Rol rol) {
		Usuario u = new Usuario();
		u.id = id != null ? id : UUID.randomUUID();
		u.dni = dni;
		u.nombres = nombres;
		u.email = email;
		u.passwordHash = passwordHash;
		u.rol = rol;
		u.creadoEn = Instant.now();
		return u;
	}
}
