package com.teleticket.teletickets.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleticket.teletickets.entity.Ticket;

/**
 * Acceso a la tabla ticket.
 */
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

	/** Las entradas de una orden, en orden de numero. */
	List<Ticket> findByOrdenIdOrderByNumero(UUID ordenId);

	/**
	 * Inserta la entrada {@code numero} de la orden, salvo que ya exista.
	 *
	 * Devuelve 1 si la creo y 0 si esa entrada ya estaba (UNIQUE(orden_id, numero)). Es la idempotencia de la
	 * emision: un orden.pagada repetido, o dos copias llegando a la vez, no pueden crear entradas de mas.
	 * Hay que llamarlo dentro de una transaccion.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			INSERT INTO ticket (id, orden_id, numero, usuario_id, evento_id, zona_id, codigo, firma,
			                    condiciones_ingreso, estado, emitido_en)
			VALUES (:id, :ordenId, :numero, :usuarioId, :eventoId, :zonaId, :codigo, :firma,
			        :condiciones, 'EMITIDO', now())
			ON CONFLICT (orden_id, numero) DO NOTHING
			""", nativeQuery = true)
	int emitir(@Param("id") UUID id, @Param("ordenId") UUID ordenId, @Param("numero") int numero,
			@Param("usuarioId") UUID usuarioId, @Param("eventoId") UUID eventoId, @Param("zonaId") UUID zonaId,
			@Param("codigo") UUID codigo, @Param("firma") String firma, @Param("condiciones") String condiciones);

	/**
	 * Anula las entradas de la orden que siguen EMITIDO (fase 11, la orden se reembolso). Devuelve cuantas.
	 *
	 * Solo toca las EMITIDO: una USADO ya se uso en la puerta y una ANULADO ya esta anulada, asi que repetir
	 * el mensaje no cambia nada. Hay que llamarlo dentro de una transaccion.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = "UPDATE ticket SET estado = 'ANULADO' WHERE orden_id = :ordenId AND estado = 'EMITIDO'",
			nativeQuery = true)
	int anularDeOrden(@Param("ordenId") UUID ordenId);
}
