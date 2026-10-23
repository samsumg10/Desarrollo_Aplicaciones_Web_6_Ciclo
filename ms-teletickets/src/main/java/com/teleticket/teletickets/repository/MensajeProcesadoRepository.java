package com.teleticket.teletickets.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleticket.teletickets.entity.MensajeProcesado;

/**
 * Registro de mensajes ya atendidos.
 */
public interface MensajeProcesadoRepository extends JpaRepository<MensajeProcesado, UUID> {

	/**
	 * Anota el mensaje como atendido. Devuelve 1 la primera vez y 0 si ya estaba. Hay que llamarlo dentro
	 * de una transaccion.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			INSERT INTO mensaje_procesado (evento_id, procesado_en)
			VALUES (:eventoId, now())
			ON CONFLICT (evento_id) DO NOTHING
			""", nativeQuery = true)
	int registrar(@Param("eventoId") UUID eventoId);
}
