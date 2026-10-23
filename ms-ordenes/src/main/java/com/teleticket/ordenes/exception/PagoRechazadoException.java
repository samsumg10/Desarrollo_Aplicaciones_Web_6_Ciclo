package com.teleticket.ordenes.exception;

public class PagoRechazadoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PagoRechazadoException(String motivo) {
		super(motivo);
	}
}
