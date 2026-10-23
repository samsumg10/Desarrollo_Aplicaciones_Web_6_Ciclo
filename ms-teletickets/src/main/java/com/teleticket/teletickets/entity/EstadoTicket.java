package com.teleticket.teletickets.entity;

/**
 * Ciclo de vida de una entrada.
 *
 *   EMITIDO --> USADO      (fase 14: se valida en la puerta, una sola vez)
 *   EMITIDO --> ANULADO    (fase 11: la orden se reembolso)
 *
 * La fase 10 solo crea entradas EMITIDO.
 */
public enum EstadoTicket {

	/** Firmada y lista para usarse. */
	EMITIDO,

	/** Ya se escaneo en la puerta. */
	USADO,

	/** Dejo de valer (reembolso). */
	ANULADO
}
