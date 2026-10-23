package com.teleticket.pagos.entity;

/**
 * Ciclo de vida de un pago.
 *
 *   PENDIENTE ---> APROBADO ---> REEMBOLSADO
 *       |  ^
 *       v  |
 *    RECHAZADO   (otro intento con otra tarjeta: la misma fila vuelve a PENDIENTE)
 */
public enum EstadoPago {

	/** Se reservo la clave de idempotencia y se esta cobrando. */
	PENDIENTE,

	/** Cobrado. Repetir el cobro de la misma orden devuelve este mismo pago. */
	APROBADO,

	/** La pasarela lo rechazo. El comprador puede reintentar. */
	RECHAZADO,

	/** Se devolvio el dinero (fase 11). */
	REEMBOLSADO
}
