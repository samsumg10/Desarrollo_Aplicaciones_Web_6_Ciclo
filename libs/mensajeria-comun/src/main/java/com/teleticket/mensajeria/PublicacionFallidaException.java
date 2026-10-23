package com.teleticket.mensajeria;

/**
 * El broker no confirmo la recepcion del mensaje, ni tras los reintentos.
 *
 * Quien publica debe tratarlo como un fallo de la operacion completa y no
 * dejarla a medias: por ejemplo, ms-eventos no puede dar el evento por
 * publicado si nadie se entero.
 */
public class PublicacionFallidaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PublicacionFallidaException(String mensaje, Throwable causa) {
		super(mensaje, causa);
	}
}
