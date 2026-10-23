package com.teleticket.mensajeria.mensajes;

import java.util.UUID;

/**
 * Datos de orden.reembolsada (productor: ms-ordenes).
 *
 * Consumidor: ms-inventario (devuelve el stock), ms-tickets (anula entradas) y ms-notificaciones.
 *
 * Contrato provisional: los campos se confirman al construir el servicio que
 * lo consume. Agregar un campo es compatible; quitar o renombrar uno obliga a
 * actualizar a todos los consumidores.
 */
public record OrdenReembolsada(
		UUID ordenId,
		UUID usuarioId,
		UUID eventoId,
		UUID zonaId,
		Integer cantidad,
		UUID reservaId) {
}
