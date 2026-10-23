package com.teleticket.eventos.exception;

/**
 * Se pidio algo que no existe. El manejador global la traduce a HTTP 404.
 */
public class NoEncontradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NoEncontradoException(String mensaje) {
		super(mensaje);
	}
}
