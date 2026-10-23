package com.teleticket.mensajeria.mensajes;

import java.util.UUID;

/**
 * Datos de emision.fallida (productor: ms-tickets).
 *
 * Consumidor: ms-ordenes (inicia la compensacion).
 *
 * Se publica cuando la emision agoto sus reintentos (RN-08).
 *
 * Contrato provisional: los campos se confirman al construir el servicio que
 * lo consume. Agregar un campo es compatible; quitar o renombrar uno obliga a
 * actualizar a todos los consumidores.
 */
public record EmisionFallida(
		UUID ordenId,
		String motivo) {
}
