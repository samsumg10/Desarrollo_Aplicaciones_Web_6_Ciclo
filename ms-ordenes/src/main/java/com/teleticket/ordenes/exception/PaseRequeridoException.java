package com.teleticket.ordenes.exception;

public class PaseRequeridoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PaseRequeridoException(String mensaje) {
		super(mensaje);
	}
}
