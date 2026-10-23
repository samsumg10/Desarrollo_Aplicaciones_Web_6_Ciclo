package com.teleticket.inventario.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleticket.inventario.entity.MensajeProcesado;

/**
 * Registro de mensajes ya atendidos (idempotencia).
 */
public interface MensajeProcesadoRepository extends JpaRepository<MensajeProcesado, UUID> {

	/**
	 * Anota el mensaje como procesado.
	 *
	 * Devuelve 1 si es la primera vez que se ve y 0 si ya estaba: en ese caso
	 * es un duplicado y el consumidor lo descarta.
	 *
	 * Insertar y comprobar es UN SOLO paso. Hacer "existsById y luego save"
	 * dejaria un hueco: dos copias del mismo mensaje que lleguen a la vez
	 * pasarian las dos la comprobacion. Con la PRIMARY KEY y ON CONFLICT, solo
	 * una inserta.
	 *
	 * Se llama dentro de la misma transaccion que guarda el trabajo, asi que
	 * si el trabajo falla, esta marca se deshace y el mensaje se podra
	 * reintentar.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			INSERT INTO mensaje_procesado (evento_id, procesado_en)
			VALUES (:eventoId, now())
			ON CONFLICT (evento_id) DO NOTHING
			""", nativeQuery = true)
	int registrar(@Param("eventoId") UUID eventoId);
}
