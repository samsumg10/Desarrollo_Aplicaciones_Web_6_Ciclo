package com.teleticket.eventos.exception;

public class NoEncontradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NoEncontradoException(String mensaje) {
		super(mensaje);
	}
}
