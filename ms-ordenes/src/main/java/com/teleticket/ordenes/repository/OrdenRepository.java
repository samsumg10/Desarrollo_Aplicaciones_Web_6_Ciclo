package com.teleticket.ordenes.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleticket.ordenes.entity.EstadoOrden;
import com.teleticket.ordenes.entity.Orden;

/**
 * Acceso a la tabla orden.
 */
public interface OrdenRepository extends JpaRepository<Orden, UUID> {

	boolean existsByPaseCodigo(UUID paseCodigo);

	/** Las ordenes de una persona, la mas reciente primero. */
	List<Orden> findByUsuarioIdOrderByCreadaEnDesc(UUID usuarioId);

	/**
	 * Cambia el estado SOLO si la orden sigue en el estado esperado.
	 *
	 * Devuelve 1 si cambio y 0 si otra peticion se adelanto. La comprobacion y el cambio van en un
	 * solo UPDATE (igual que la reserva en ms-inventario), asi que dos pagos simultaneos de la
	 * misma orden no pueden los dos "aprobarla" ni publicar dos veces orden.pagada.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("UPDATE Orden o SET o.estado = :hacia WHERE o.id = :id AND o.estado = :desde")
	int cambiarEstado(@Param("id") UUID id, @Param("desde") EstadoOrden desde, @Param("hacia") EstadoOrden hacia);
}
