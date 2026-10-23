package com.teleticket.mensajeria.mensajes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Datos de reembolso.solicitado (productor: ms-ordenes).
 *
 * Consumidor: ms-pagos (devuelve el dinero).
 *
 * Contrato provisional: los campos se confirman al construir el servicio que
 * lo consume. Agregar un campo es compatible; quitar o renombrar uno obliga a
 * actualizar a todos los consumidores.
 */
public record ReembolsoSolicitado(
		UUID ordenId,
		BigDecimal monto,
		String motivo) {
}
