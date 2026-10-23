package com.teleticket.inventario.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleticket.inventario.entity.EstadoReserva;
import com.teleticket.inventario.entity.Reserva;

/**
 * Acceso a la tabla reserva.
 */
public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

	Optional<Reserva> findByOrdenId(UUID ordenId);

	/**
	 * Cambia el estado SOLO si la reserva sigue en el estado esperado.
	 *
	 * Devuelve 1 si cambio y 0 si otra operacion se adelanto. Es el mismo
	 * truco que el descuento de stock: la comprobacion y el cambio van en un
	 * solo UPDATE, asi que no hay hueco entre "mirar el estado" y "cambiarlo"
	 * por el que pueda colarse otro proceso (por ejemplo, el vencimiento).
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("UPDATE Reserva r SET r.estado = :hacia WHERE r.id = :id AND r.estado = :desde")
	int cambiarEstado(@Param("id") UUID id,
			@Param("desde") EstadoReserva desde,
			@Param("hacia") EstadoReserva hacia);
}
