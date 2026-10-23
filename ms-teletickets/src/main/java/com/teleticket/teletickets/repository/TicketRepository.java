package com.teleticket.teletickets.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleticket.teletickets.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

	List<Ticket> findByOrdenIdOrderByNumero(UUID ordenId);

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

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = "UPDATE ticket SET estado = 'ANULADO' WHERE orden_id = :ordenId AND estado = 'EMITIDO'",
			nativeQuery = true)
	int anularDeOrden(@Param("ordenId") UUID ordenId);
}
