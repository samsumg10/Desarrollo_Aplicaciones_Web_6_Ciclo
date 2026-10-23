package com.teleticket.mensajeria.mensajes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Datos de pago.reembolsado (productor: ms-pagos).
 *
 * Consumidor: ms-ordenes (marca REEMBOLSADA).
 *
 * Contrato provisional: los campos se confirman al construir el servicio que
 * lo consume. Agregar un campo es compatible; quitar o renombrar uno obliga a
 * actualizar a todos los consumidores.
 */
public record PagoReembolsado(
		UUID ordenId,
		BigDecimal monto) {
}
