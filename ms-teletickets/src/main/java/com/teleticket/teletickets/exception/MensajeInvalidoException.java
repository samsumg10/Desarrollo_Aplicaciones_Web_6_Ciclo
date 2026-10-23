package com.teleticket.teletickets.exception;

/**
 * Llego un mensaje al que le faltan datos esenciales (sin orden, sin cantidad...).
 *
 * Reintentarlo no lo arregla: el contenido seguira estando mal. Por eso el listener lo descarta (reject sin
 * volver a la cola) en vez de reintentar y esperar 7 segundos para nada.
 */
public class MensajeInvalidoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MensajeInvalidoException(String mensaje) {
		super(mensaje);
	}
}
