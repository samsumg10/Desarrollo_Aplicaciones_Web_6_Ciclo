package com.teleticket.ordenes.exception;

/**
 * ms-inventario no tiene entradas suficientes en la zona. Es 409.
 *
 * Tiene clase propia porque OrdenService, al recibirla, debe DEVOLVER el pase
 * que ya consumio: el comprador no puede perder su turno por falta de stock.
 */
public class SinStockException extends ReglaNegocioException {

	private static final long serialVersionUID = 1L;

	public SinStockException(String mensaje) {
		super("SIN_STOCK", mensaje);
	}
}
