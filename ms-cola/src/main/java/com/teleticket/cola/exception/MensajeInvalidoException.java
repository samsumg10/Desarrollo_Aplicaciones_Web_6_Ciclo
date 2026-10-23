package com.teleticket.cola.exception;

/**
 * Llego un mensaje al que le faltan datos esenciales (sin zonas, sin id...).
 *
 * A diferencia de un fallo de base de datos, reintentarlo no lo arregla: el
 * contenido seguira estando mal. Por eso el listener lo descarta (reject sin
 * volver a la cola) en vez de devolverlo y entrar en un bucle infinito.
 */
public class MensajeInvalidoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MensajeInvalidoException(String mensaje) {
		super(mensaje);
	}
}
