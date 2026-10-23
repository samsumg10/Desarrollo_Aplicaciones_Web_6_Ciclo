package com.teleticket.mensajeria.mensajes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Datos de orden.pagada (productor: ms-ordenes).
 *
 * Consumidor: ms-inventario (confirma la reserva) y ms-tickets (emite los e-tickets).
 *
 * Contrato provisional: los campos se confirman al construir el servicio que
 * lo consume. Agregar un campo es compatible; quitar o renombrar uno obliga a
 * actualizar a todos los consumidores.
 */
public record OrdenPagada(
		UUID ordenId,
		UUID usuarioId,
		UUID eventoId,
		UUID zonaId,
		Integer cantidad,
		UUID reservaId,
		BigDecimal total) {
}
