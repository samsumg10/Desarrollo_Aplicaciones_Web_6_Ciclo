package com.teleticket.cola.exception;

/** Falta o es invalida la cabecera X-Usuario-Id -> HTTP 401. */
public class NoAutenticadoException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public NoAutenticadoException(String mensaje) {
		super(mensaje);
	}
}
