package com.teleticket.pagos.exception;

/**
 * Llego un mensaje al que le faltan datos esenciales, o que pide reembolsar un pago que no existe o que no se cobro.
 *
 * Reintentarlo no lo arregla: el listener lo descarta (reject sin volver a la cola) en vez de devolverlo
 * y entrar en un bucle infinito.
 */
public class MensajeInvalidoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MensajeInvalidoException(String mensaje) {
		super(mensaje);
	}
}
