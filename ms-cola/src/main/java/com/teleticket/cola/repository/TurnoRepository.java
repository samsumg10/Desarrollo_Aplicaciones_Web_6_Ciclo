package com.teleticket.cola.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleticket.cola.entity.EstadoTurno;
import com.teleticket.cola.entity.Turno;

public interface TurnoRepository extends JpaRepository<Turno, UUID> {

	Optional<Turno> findByEventoIdAndUsuarioId(UUID eventoId, UUID usuarioId);

	List<Turno> findByEventoIdAndEstadoOrderByIngresoEnAscIdAsc(UUID eventoId, EstadoTurno estado);

	long countByEventoIdAndEstadoAndPosicionLessThan(UUID eventoId, EstadoTurno estado, Integer posicion);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE turno t SET posicion = s.rn, estado = 'EN_COLA'
			  FROM (SELECT id, row_number() OVER (ORDER BY random()) AS rn
			          FROM turno
			         WHERE evento_id = :eventoId AND estado = 'EN_SALA') s
			 WHERE t.id = s.id
			""", nativeQuery = true)
	int sortear(@Param("eventoId") UUID eventoId);

	@Query(value = "SELECT DISTINCT evento_id FROM turno WHERE estado = 'EN_COLA'", nativeQuery = true)
	List<UUID> eventosConTurnosEnCola();

	@Query(value = """
			SELECT * FROM turno
			 WHERE evento_id = :eventoId AND estado = 'EN_COLA'
			 ORDER BY posicion
			 LIMIT :limite
			   FOR UPDATE SKIP LOCKED
			""", nativeQuery = true)
	List<Turno> siguientesEnCola(@Param("eventoId") UUID eventoId, @Param("limite") int limite);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE turno SET estado = 'EXPIRADO'
			 WHERE estado = 'HABILITADO'
			   AND id IN (SELECT turno_id FROM pase_compra WHERE estado = 'VENCIDO')
			""", nativeQuery = true)
	int expirarConPaseVencido();
}
