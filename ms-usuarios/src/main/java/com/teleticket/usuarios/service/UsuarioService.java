package com.teleticket.usuarios.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teleticket.usuarios.dto.ContactoResponse;
import com.teleticket.usuarios.dto.RegistroRequest;
import com.teleticket.usuarios.dto.UsuarioResponse;
import com.teleticket.usuarios.entity.Rol;
import com.teleticket.usuarios.entity.Usuario;
import com.teleticket.usuarios.exception.NoEncontradoException;
import com.teleticket.usuarios.exception.ReglaNegocioException;
import com.teleticket.usuarios.repository.UsuarioRepository;

/** Reglas de CU-01 (registro). El login con JWT llega con Spring Security. */
@Service
public class UsuarioService {

	private final UsuarioRepository repo;
	private final PasswordEncoder encoder;

	public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder) {
		this.repo = repo;
		this.encoder = encoder;
	}

	@Transactional
	public UsuarioResponse registrar(RegistroRequest req) {
		// El correo se guarda en minusculas: Ana@x.com y ana@x.com son la misma cuenta.
		String email = req.email().trim().toLowerCase(Locale.ROOT);

		if (repo.existsByDni(req.dni())) {
			throw new ReglaNegocioException("DNI_DUPLICADO", "Ya existe un usuario con ese DNI");
		}
		if (repo.existsByEmail(email)) {
			throw new ReglaNegocioException("EMAIL_DUPLICADO", "Ya existe un usuario con ese correo");
		}

		// Siempre COMPRADOR: ver el comentario de RegistroRequest.
		Usuario u = Usuario.crear(null, req.dni(), req.nombres().trim(), email,
				encoder.encode(req.password()), Rol.COMPRADOR);
		return UsuarioResponse.de(repo.save(u));
	}

	@Transactional(readOnly = true)
	public UsuarioResponse perfil(UUID id) {
		return UsuarioResponse.de(buscar(id));
	}

	@Transactional(readOnly = true)
	public ContactoResponse contacto(UUID id) {
		Usuario u = buscar(id);
		return new ContactoResponse(u.getNombres(), u.getEmail());
	}

	private Usuario buscar(UUID id) {
		return repo.findById(id)
				.orElseThrow(() -> new NoEncontradoException("No existe el usuario " + id));
	}
}
