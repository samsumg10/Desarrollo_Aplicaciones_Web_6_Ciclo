package com.teleticket.ordenes.exception;

/**
 * La pasarela rechazo la tarjeta. Es 402 (Payment Required).
 *
 * No es un fallo del sistema: la orden vuelve a RESERVADA con su reserva intacta, asi que el
 * comprador puede reintentar con otra tarjeta mientras no venza.
 */
public class PagoRechazadoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PagoRechazadoException(String motivo) {
		super(motivo);
	}
}
