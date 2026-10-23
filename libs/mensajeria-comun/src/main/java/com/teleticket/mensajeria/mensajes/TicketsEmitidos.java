package com.teleticket.mensajeria.mensajes;

import java.util.List;
import java.util.UUID;

/**
 * Datos de tickets.emitidos (productor: ms-tickets).
 *
 * Consumidor: ms-ordenes (marca EMITIDA) y ms-notificaciones (envia el correo).
 *
 * Contrato provisional: los campos se confirman al construir el servicio que
 * lo consume. Agregar un campo es compatible; quitar o renombrar uno obliga a
 * actualizar a todos los consumidores.
 */
public record TicketsEmitidos(
		UUID ordenId,
		UUID usuarioId,
		UUID eventoId,
		List<UUID> ticketIds) {
}
