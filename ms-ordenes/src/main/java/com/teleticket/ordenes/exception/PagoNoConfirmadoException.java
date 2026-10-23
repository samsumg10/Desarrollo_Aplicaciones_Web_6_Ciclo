package com.teleticket.ordenes.exception;

public class PagoNoConfirmadoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PagoNoConfirmadoException(String detalle, Throwable causa) {
		super(detalle, causa);
	}
}
