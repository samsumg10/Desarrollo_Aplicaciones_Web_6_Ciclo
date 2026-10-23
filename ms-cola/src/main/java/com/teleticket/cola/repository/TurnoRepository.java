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

/**
 * Acceso a la tabla turno.
 */
public interface TurnoRepository extends JpaRepository<Turno, UUID> {

	Optional<Turno> findByEventoIdAndUsuarioId(UUID eventoId, UUID usuarioId);

	/** Los que esperan sin posicion, en orden de llegada. */
	List<Turno> findByEventoIdAndEstadoOrderByIngresoEnAscIdAsc(UUID eventoId, EstadoTurno estado);

	/** Cuantos turnos EN_COLA hay con una posicion menor: los que van delante. */
	long countByEventoIdAndEstadoAndPosicionLessThan(UUID eventoId, EstadoTurno estado, Integer posicion);

	/**
	 * EL SORTEO: asigna una posicion aleatoria del 1 a N a todos los EN_SALA, en UN solo SQL.
	 *
	 * Un solo UPDATE es una sola operacion atomica: o todos reciben su posicion
	 * o ninguno. No existe un estado intermedio con la cola a medio ordenar.
	 * 'ORDER BY random()' baraja a los participantes y row_number() numera el
	 * resultado. Devuelve cuantos turnos sorteo.
	 *
	 * Se llama justo despues de reclamar el sorteo, dentro de la misma transaccion.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE turno t SET posicion = s.rn, estado = 'EN_COLA'
			  FROM (SELECT id, row_number() OVER (ORDER BY random()) AS rn
			          FROM turno
			         WHERE evento_id = :eventoId AND estado = 'EN_SALA') s
			 WHERE t.id = s.id
			""", nativeQuery = true)
	int sortear(@Param("eventoId") UUID eventoId);

	/** Los eventos que tienen gente esperando su ciclo. */
	@Query(value = "SELECT DISTINCT evento_id FROM turno WHERE estado = 'EN_COLA'", nativeQuery = true)
	List<UUID> eventosConTurnosEnCola();

	/**
	 * Los siguientes 'limite' turnos de la cola, por posicion, ya bloqueados.
	 *
	 * 'FOR UPDATE SKIP LOCKED': si otra replica esta habilitando a los primeros,
	 * esta se salta los bloqueados y toma los siguientes. Dos replicas nunca
	 * habilitan al mismo turno.
	 */
	@Query(value = """
			SELECT * FROM turno
			 WHERE evento_id = :eventoId AND estado = 'EN_COLA'
			 ORDER BY posicion
			 LIMIT :limite
			   FOR UPDATE SKIP LOCKED
			""", nativeQuery = true)
	List<Turno> siguientesEnCola(@Param("eventoId") UUID eventoId, @Param("limite") int limite);

	/** Los turnos cuyo pase vencio sin usarse pasan a EXPIRADO. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE turno SET estado = 'EXPIRADO'
			 WHERE estado = 'HABILITADO'
			   AND id IN (SELECT turno_id FROM pase_compra WHERE estado = 'VENCIDO')
			""", nativeQuery = true)
	int expirarConPaseVencido();
}
