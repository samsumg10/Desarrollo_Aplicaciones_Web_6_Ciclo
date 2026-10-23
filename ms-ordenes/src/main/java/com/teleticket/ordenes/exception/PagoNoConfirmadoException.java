package com.teleticket.ordenes.exception;

/**
 * ms-pagos no respondio a tiempo y NO SE SABE si cobro. Es 503.
 *
 * Es distinto de un rechazo: el cobro pudo haberse hecho. Por eso la orden NO vuelve a RESERVADA
 * (liberaria la reserva de alguien que quiza ya pago): se queda en PENDIENTE_PAGO. Reintentar es
 * seguro, porque ms-pagos es idempotente: si ya habia cobrado, devuelve ese pago sin cobrar otra vez.
 */
public class PagoNoConfirmadoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PagoNoConfirmadoException(String detalle, Throwable causa) {
		super(detalle, causa);
	}
}
