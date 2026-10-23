package com.teleticket.ordenes.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleticket.ordenes.entity.EstadoOrden;
import com.teleticket.ordenes.entity.Orden;

public interface OrdenRepository extends JpaRepository<Orden, UUID> {

	boolean existsByPaseCodigo(UUID paseCodigo);

	List<Orden> findByUsuarioIdOrderByCreadaEnDesc(UUID usuarioId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("UPDATE Orden o SET o.estado = :hacia WHERE o.id = :id AND o.estado = :desde")
	int cambiarEstado(@Param("id") UUID id, @Param("desde") EstadoOrden desde, @Param("hacia") EstadoOrden hacia);
}
