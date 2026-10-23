package com.teleticket.mensajeria;

public class PublicacionFallidaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PublicacionFallidaException(String mensaje, Throwable causa) {
		super(mensaje, causa);
	}
}
