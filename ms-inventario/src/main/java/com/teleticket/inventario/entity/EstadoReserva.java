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
 * ACTIVA <-> EN_PAGO lo mueve ms-ordenes por la API interna (fase 8/9).
 * CONFIRMADA la produce el consumidor de orden.pagada (fase 9). LIBERADA la
 * producen los de reserva.vencida y orden.reembolsada, que llegan en fases
 * posteriores.
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
