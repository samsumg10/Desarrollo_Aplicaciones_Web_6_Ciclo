package com.teleticket.ordenes.exception;

public class SinStockException extends ReglaNegocioException {

	private static final long serialVersionUID = 1L;

	public SinStockException(String mensaje) {
		super("SIN_STOCK", mensaje);
	}
}
