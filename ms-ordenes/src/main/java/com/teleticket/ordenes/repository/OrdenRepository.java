package com.teleticket.ordenes.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teleticket.ordenes.entity.Orden;

/**
 * Acceso a la tabla orden.
 */
public interface OrdenRepository extends JpaRepository<Orden, UUID> {

	boolean existsByPaseCodigo(UUID paseCodigo);

	/** Las ordenes de una persona, la mas reciente primero. */
	List<Orden> findByUsuarioIdOrderByCreadaEnDesc(UUID usuarioId);
}
