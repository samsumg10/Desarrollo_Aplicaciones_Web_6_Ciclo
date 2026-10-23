package com.teleticket.mensajeria.mensajes;

import java.util.UUID;

/**
 * Datos de reserva.liberada (productor: ms-inventario).
 *
 * Consumidor: ms-ordenes (marca la orden EXPIRADA).
 *
 * Contrato provisional: los campos se confirman al construir el servicio que
 * lo consume. Agregar un campo es compatible; quitar o renombrar uno obliga a
 * actualizar a todos los consumidores.
 */
public record ReservaLiberada(
		UUID reservaId,
		UUID ordenId,
		UUID zonaId,
		Integer cantidad) {
}
