package com.teleticket.inventario.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleticket.inventario.entity.StockZona;

public interface StockZonaRepository extends JpaRepository<StockZona, UUID> {

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE stock_zona
			   SET disponible = disponible - :cantidad,
			       version    = version + 1
			 WHERE zona_id = :zonaId
			   AND disponible >= :cantidad
			""", nativeQuery = true)
	int descontar(@Param("zonaId") UUID zonaId, @Param("cantidad") int cantidad);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			INSERT INTO stock_zona (zona_id, evento_id, total, disponible, version)
			VALUES (:zonaId, :eventoId, :aforo, :aforo, 0)
			ON CONFLICT (zona_id) DO NOTHING
			""", nativeQuery = true)
	int crearSiNoExiste(@Param("zonaId") UUID zonaId,
			@Param("eventoId") UUID eventoId,
			@Param("aforo") int aforo);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE stock_zona
			   SET disponible = disponible + :cantidad,
			       version    = version + 1
			 WHERE zona_id = :zonaId
			""", nativeQuery = true)
	int reponer(@Param("zonaId") UUID zonaId, @Param("cantidad") int cantidad);
}
