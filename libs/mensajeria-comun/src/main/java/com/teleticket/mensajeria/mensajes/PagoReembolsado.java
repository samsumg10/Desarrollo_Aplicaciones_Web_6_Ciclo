package com.teleticket.mensajeria.mensajes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Datos de pago.reembolsado (productor: ms-pagos).
 *
 * Consumidor: ms-ordenes (marca REEMBOLSADA).
 *
 * Fase 11: se agrego 'motivo' (el que trajo reembolso.solicitado) para que ms-ordenes lo deje en orden_historial.
 * Es compatible: un mensaje sin el campo se lee con motivo null.
 *
 * Contrato provisional: los campos se confirman al construir el servicio que
 * lo consume. Agregar un campo es compatible; quitar o renombrar uno obliga a
 * actualizar a todos los consumidores.
 */
public record PagoReembolsado(
		UUID ordenId,
		BigDecimal monto,
		String motivo) {
}
