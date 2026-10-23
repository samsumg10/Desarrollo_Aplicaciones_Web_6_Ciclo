package com.teleticket.usuarios.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teleticket.usuarios.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

	boolean existsByDni(String dni);

	boolean existsByEmail(String email);
}
