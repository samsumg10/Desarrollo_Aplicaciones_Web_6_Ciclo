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

/**
 * Acceso a la copia local de los eventos.
 */
public interface VentaEventoRepository extends JpaRepository<VentaEvento, UUID> {

	/**
	 * Guarda la copia local, o no hace nada si ya existe.
	 *
	 * No se usa save(): con un id asignado a mano haria un "merge" y podria
	 * pisar el estado del sorteo de un evento que ya se sorteo.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			INSERT INTO venta_evento (evento_id, nombre, inicio_venta, estado_sorteo,
			                          capacidad_por_ciclo, ultima_posicion, creado_en)
			VALUES (:eventoId, :nombre, :inicioVenta, 'PENDIENTE', :capacidad, 0, now())
			ON CONFLICT (evento_id) DO NOTHING
			""", nativeQuery = true)
	int crearSiNoExiste(@Param("eventoId") UUID eventoId, @Param("nombre") String nombre,
			@Param("inicioVenta") Instant inicioVenta, @Param("capacidad") int capacidad);

	/** Los eventos cuya hora de venta ya llego y todavia no se sortearon. */
	@Query(value = """
			SELECT evento_id FROM venta_evento
			 WHERE estado_sorteo = 'PENDIENTE' AND inicio_venta <= now()
			""", nativeQuery = true)
	List<UUID> pendientesDeSorteo();

	/**
	 * Reclama el sorteo: PENDIENTE -> REALIZADO, y solo UNA vez.
	 *
	 * Devuelve 1 a quien lo reclama y 0 a todos los demas. Es la garantia de que
	 * el sorteo es unico aunque la tarea corra dos veces a la vez o haya dos
	 * replicas del servicio: la segunda espera el bloqueo de la fila, vuelve a
	 * evaluar 'estado_sorteo = PENDIENTE', ya no lo cumple y afecta 0 filas.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE venta_evento SET estado_sorteo = 'REALIZADO', sorteo_en = now()
			 WHERE evento_id = :eventoId AND estado_sorteo = 'PENDIENTE'
			""", nativeQuery = true)
	int reclamarSorteo(@Param("eventoId") UUID eventoId);

	/** Deja el contador en la mayor posicion repartida, para que las llegadas tardias sigan desde ahi. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE venta_evento
			   SET ultima_posicion = COALESCE((SELECT max(posicion) FROM turno WHERE evento_id = :eventoId), 0)
			 WHERE evento_id = :eventoId
			""", nativeQuery = true)
	int fijarUltimaPosicion(@Param("eventoId") UUID eventoId);

	/**
	 * Toma la siguiente posicion SOLO si el sorteo ya se hizo (RN-02).
	 *
	 * Devuelve 1 si la tomo y 0 si el sorteo aun esta pendiente (o el evento no
	 * existe). Un solo UPDATE: dos llegadas simultaneas nunca reciben la misma
	 * posicion, porque la segunda espera la fila y suma sobre el valor ya subido.
	 * Despues de este UPDATE, ultimaPosicion() lee el valor propio: la fila sigue
	 * bloqueada por esta transaccion, nadie lo puede cambiar.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE venta_evento SET ultima_posicion = ultima_posicion + 1
			 WHERE evento_id = :eventoId AND estado_sorteo = 'REALIZADO'
			""", nativeQuery = true)
	int tomarPosicionTardia(@Param("eventoId") UUID eventoId);

	@Query(value = "SELECT ultima_posicion FROM venta_evento WHERE evento_id = :eventoId", nativeQuery = true)
	int ultimaPosicion(@Param("eventoId") UUID eventoId);

	/** Eventos ya sorteados con alguien que quedo EN_SALA (entro justo durante el sorteo). */
	@Query(value = """
			SELECT DISTINCT v.evento_id FROM venta_evento v
			  JOIN turno t ON t.evento_id = v.evento_id
			 WHERE v.estado_sorteo = 'REALIZADO' AND t.estado = 'EN_SALA'
			""", nativeQuery = true)
	List<UUID> conRezagados();

	/** Carga el evento bloqueando su fila hasta el fin de la transaccion (SELECT ... FOR UPDATE). */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT v FROM VentaEvento v WHERE v.eventoId = :eventoId")
	Optional<VentaEvento> bloquear(@Param("eventoId") UUID eventoId);
}
