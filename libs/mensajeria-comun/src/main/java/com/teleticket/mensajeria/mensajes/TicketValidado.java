package com.teleticket.mensajeria.mensajes;

import java.time.Instant;
import java.util.UUID;

/**
 * Datos de ticket.validado (productor: ms-tickets).
 *
 * Consumidor: ninguno por ahora (queda listo para reportes con Kafka, seccion 11).
 *
 * Contrato provisional: los campos se confirman al construir el servicio que
 * lo consume. Agregar un campo es compatible; quitar o renombrar uno obliga a
 * actualizar a todos los consumidores.
 */
public record TicketValidado(
		UUID ticketId,
		UUID ordenId,
		UUID eventoId,
		UUID controladorId,
		Instant validadoEn) {
}
