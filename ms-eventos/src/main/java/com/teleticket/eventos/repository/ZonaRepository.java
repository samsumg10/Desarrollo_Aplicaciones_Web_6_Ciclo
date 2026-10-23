package com.teleticket.eventos.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teleticket.eventos.entity.Zona;

public interface ZonaRepository extends JpaRepository<Zona, UUID> {

	Optional<Zona> findByIdAndEventoId(UUID id, UUID eventoId);

	boolean existsByEventoIdAndNombreIgnoreCase(UUID eventoId, String nombre);
}
