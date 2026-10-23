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
	REEMBOLSADA;

	/**
	 * Si las reglas del ciclo de vida permiten pasar de este estado a {@code hacia}.
	 *
	 * Es la tabla de transiciones en un solo sitio (fase 11). No reemplaza al UPDATE condicional de
	 * OrdenRepository.cambiarEstado, que es lo que protege de las peticiones simultaneas: sirve para decidir
	 * antes de escribir y para dar un mensaje claro. Lo importante: EXPIRADA y REEMBOLSADA son finales, asi
	 * que un tickets.emitidos tardio no puede resucitar una orden ya reembolsada.
	 *
	 * REEMBOLSADA se admite desde PAGADA y desde EMITIDA: si el dinero ya se devolvio, la orden lo refleja
	 * aunque las entradas hubieran alcanzado a emitirse (se anulan al llegar orden.reembolsada).
	 */
	public boolean puedePasarA(EstadoOrden hacia) {
		return switch (this) {
			case RESERVADA -> hacia == PENDIENTE_PAGO || hacia == EXPIRADA;
			case PENDIENTE_PAGO -> hacia == PAGADA || hacia == RESERVADA || hacia == EXPIRADA;
			case PAGADA -> hacia == EMITIDA || hacia == REEMBOLSADA;
			case EMITIDA -> hacia == REEMBOLSADA;
			case EXPIRADA, REEMBOLSADA -> false;
		};
	}
}
