package com.teleticket.ordenes.entity;

/**
 * Ciclo de vida de una orden (seccion 8.5 del documento de arquitectura).
 *
 *   RESERVADA --> PENDIENTE_PAGO --> PAGADA --> EMITIDA
 *       ^               |
 *       +---------------+  (pago rechazado)
 *
 *   y los finales: EXPIRADA (la reserva vencio sin pago) y REEMBOLSADA.
 *
 * La fase 8 solo crea ordenes RESERVADAS. Los demas estados los mueven la fase 9
 * (pago), la 10 (emision) y la 11 (reembolso).
 */
public enum EstadoOrden {

	/** Pase consumido y stock reservado: el comprador puede pagar hasta 'venceEn'. */
	RESERVADA,

	/** Se esta cobrando. */
	PENDIENTE_PAGO,

	/** Pago aprobado; falta emitir las entradas. */
	PAGADA,

	/** Entradas emitidas. */
	EMITIDA,

	/** La reserva vencio sin pago. */
	EXPIRADA,

	/** Se devolvio el dinero. */
	REEMBOLSADA
}
