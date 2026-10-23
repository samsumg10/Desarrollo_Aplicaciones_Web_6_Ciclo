package com.teleticket.eventos.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teleticket.eventos.entity.Zona;

/**
 * Acceso a la tabla zona.
 */
public interface ZonaRepository extends JpaRepository<Zona, UUID> {

	/**
	 * Busca una zona comprobando que pertenezca al evento indicado.
	 *
	 * Lo usa el endpoint interno que llama ms-ordenes (fase 8): pedir la zona
	 * junto con su evento evita que alguien combine un eventoId con el zonaId
	 * de otro evento y obtenga un precio que no corresponde.
	 */
	Optional<Zona> findByIdAndEventoId(UUID id, UUID eventoId);

	boolean existsByEventoIdAndNombreIgnoreCase(UUID eventoId, String nombre);
}
