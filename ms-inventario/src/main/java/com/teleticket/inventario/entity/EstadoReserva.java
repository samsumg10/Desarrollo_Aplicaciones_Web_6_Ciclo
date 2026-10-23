package com.teleticket.inventario.entity;

/**
 * Ciclo de vida de una reserva (seccion 8.4 del documento de arquitectura).
 *
 *   ACTIVA ---------> EN_PAGO ---------> CONFIRMADA
 *      ^                 |                   |
 *      +-- pago rechazado+                   |
 *      |                                     v
 *      +----------------------------------> LIBERADA
 *
 * En esta fase solo se recorre ACTIVA <-> EN_PAGO. CONFIRMADA y LIBERADA las
 * producen los consumidores de orden.pagada, reserva.vencida y
 * orden.reembolsada, que llegan en fases posteriores.
 */
public enum EstadoReserva {

	/** Las entradas estan apartadas y el comprador aun puede pagar. */
	ACTIVA,

	/** Se esta cobrando: la reserva no debe vencer a mitad del pago. */
	EN_PAGO,

	/** Pagada. El stock ya no vuelve a estar disponible. */
	CONFIRMADA,

	/** Vencida o reembolsada: las entradas volvieron al stock. */
	LIBERADA
}
