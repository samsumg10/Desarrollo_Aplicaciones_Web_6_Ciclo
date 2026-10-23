package com.teleticket.ordenes.exception;

/**
 * El pase de compra no sirve: no existe, es de otra persona o de otro evento, ya
 * se uso o vencio. Es 409, como cualquier regla de negocio.
 *
 * Tiene clase propia porque OrdenService reacciona distinto ante ella: un pase
 * invalido significa que NO se consumio, asi que no hay nada que devolver.
 */
public class PaseNoValidoException extends ReglaNegocioException {

	private static final long serialVersionUID = 1L;

	public PaseNoValidoException(String mensaje) {
		super("PASE_NO_VALIDO", mensaje);
	}
}
