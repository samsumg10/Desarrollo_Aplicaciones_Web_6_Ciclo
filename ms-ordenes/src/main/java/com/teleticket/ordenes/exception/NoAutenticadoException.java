package com.teleticket.ordenes.exception;

public class NoAutenticadoException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public NoAutenticadoException(String mensaje) {
		super(mensaje);
	}
}
