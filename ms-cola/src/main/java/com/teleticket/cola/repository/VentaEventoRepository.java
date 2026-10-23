package com.teleticket.cola.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleticket.cola.entity.VentaEvento;

import jakarta.persistence.LockModeType;

public interface VentaEventoRepository extends JpaRepository<VentaEvento, UUID> {

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			INSERT INTO venta_evento (evento_id, nombre, inicio_venta, estado_sorteo,
			                          capacidad_por_ciclo, ultima_posicion, creado_en)
			VALUES (:eventoId, :nombre, :inicioVenta, 'PENDIENTE', :capacidad, 0, now())
			ON CONFLICT (evento_id) DO NOTHING
			""", nativeQuery = true)
	int crearSiNoExiste(@Param("eventoId") UUID eventoId, @Param("nombre") String nombre,
			@Param("inicioVenta") Instant inicioVenta, @Param("capacidad") int capacidad);

	@Query(value = """
			SELECT evento_id FROM venta_evento
			 WHERE estado_sorteo = 'PENDIENTE' AND inicio_venta <= now()
			""", nativeQuery = true)
	List<UUID> pendientesDeSorteo();

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE venta_evento SET estado_sorteo = 'REALIZADO', sorteo_en = now()
			 WHERE evento_id = :eventoId AND estado_sorteo = 'PENDIENTE'
			""", nativeQuery = true)
	int reclamarSorteo(@Param("eventoId") UUID eventoId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE venta_evento
			   SET ultima_posicion = COALESCE((SELECT max(posicion) FROM turno WHERE evento_id = :eventoId), 0)
			 WHERE evento_id = :eventoId
			""", nativeQuery = true)
	int fijarUltimaPosicion(@Param("eventoId") UUID eventoId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE venta_evento SET ultima_posicion = ultima_posicion + 1
			 WHERE evento_id = :eventoId AND estado_sorteo = 'REALIZADO'
			""", nativeQuery = true)
	int tomarPosicionTardia(@Param("eventoId") UUID eventoId);

	@Query(value = "SELECT ultima_posicion FROM venta_evento WHERE evento_id = :eventoId", nativeQuery = true)
	int ultimaPosicion(@Param("eventoId") UUID eventoId);

	@Query(value = """
			SELECT DISTINCT v.evento_id FROM venta_evento v
			  JOIN turno t ON t.evento_id = v.evento_id
			 WHERE v.estado_sorteo = 'REALIZADO' AND t.estado = 'EN_SALA'
			""", nativeQuery = true)
	List<UUID> conRezagados();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT v FROM VentaEvento v WHERE v.eventoId = :eventoId")
	Optional<VentaEvento> bloquear(@Param("eventoId") UUID eventoId);
}
