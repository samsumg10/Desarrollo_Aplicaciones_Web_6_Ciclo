package com.teleticket.cola.exception;

public class MensajeInvalidoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MensajeInvalidoException(String mensaje) {
		super(mensaje);
	}
}
