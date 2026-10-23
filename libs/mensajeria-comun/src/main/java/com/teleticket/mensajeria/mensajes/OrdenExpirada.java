package com.teleticket.mensajeria.mensajes;

import java.util.UUID;

/**
 * Datos de orden.expirada (productor: ms-ordenes).
 *
 * Consumidor: ms-notificaciones (avisa que la reserva vencio sin pago).
 *
 * Contrato provisional: los campos se confirman al construir el servicio que
 * lo consume. Agregar un campo es compatible; quitar o renombrar uno obliga a
 * actualizar a todos los consumidores.
 */
public record OrdenExpirada(
		UUID ordenId,
		UUID usuarioId,
		UUID eventoId) {
}
