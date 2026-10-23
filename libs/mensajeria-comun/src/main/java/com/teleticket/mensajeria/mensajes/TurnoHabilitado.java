package com.teleticket.mensajeria.mensajes;

import java.time.Instant;
import java.util.UUID;

/**
 * Datos de turno.habilitado (productor: ms-cola).
 *
 * Consumidor: ms-notificaciones.
 *
 * Avisa al comprador que llego su turno y hasta cuando vale su pase.
 *
 * Contrato provisional: los campos se confirman al construir el servicio que
 * lo consume. Agregar un campo es compatible; quitar o renombrar uno obliga a
 * actualizar a todos los consumidores.
 */
public record TurnoHabilitado(
		UUID turnoId,
		UUID usuarioId,
		UUID eventoId,
		String paseCodigo,
		Instant paseVenceEn) {
}
