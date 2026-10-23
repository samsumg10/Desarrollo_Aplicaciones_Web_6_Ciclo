package com.teleticket.inventario.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleticket.inventario.entity.EstadoReserva;
import com.teleticket.inventario.entity.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

	Optional<Reserva> findByOrdenId(UUID ordenId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("UPDATE Reserva r SET r.estado = :hacia WHERE r.id = :id AND r.estado = :desde")
	int cambiarEstado(@Param("id") UUID id,
			@Param("desde") EstadoReserva desde,
			@Param("hacia") EstadoReserva hacia);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("UPDATE Reserva r SET r.estado = com.teleticket.inventario.entity.EstadoReserva.CONFIRMADA "
			+ "WHERE r.id = :id AND r.estado IN (com.teleticket.inventario.entity.EstadoReserva.EN_PAGO, "
			+ "com.teleticket.inventario.entity.EstadoReserva.ACTIVA)")
	int confirmar(@Param("id") UUID id);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("UPDATE Reserva r SET r.estado = com.teleticket.inventario.entity.EstadoReserva.LIBERADA "
			+ "WHERE r.id = :id AND r.estado <> com.teleticket.inventario.entity.EstadoReserva.LIBERADA")
	int liberar(@Param("id") UUID id);
}
