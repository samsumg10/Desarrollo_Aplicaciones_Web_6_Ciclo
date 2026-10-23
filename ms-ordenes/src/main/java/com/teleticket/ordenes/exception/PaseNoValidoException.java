package com.teleticket.ordenes.exception;

public class PaseNoValidoException extends ReglaNegocioException {

	private static final long serialVersionUID = 1L;

	public PaseNoValidoException(String mensaje) {
		super("PASE_NO_VALIDO", mensaje);
	}
}
