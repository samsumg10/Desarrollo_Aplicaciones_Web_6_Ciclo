package com.teleticket.ordenes.exception;

/**
 * Falta la cabecera X-Pase-Compra. Es 400.
 *
 * El gateway ya la rechaza antes de llegar aqui (primera barrera de PRB-03); esto
 * cubre las llamadas directas al puerto 8085.
 */
public class PaseRequeridoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PaseRequeridoException(String mensaje) {
		super(mensaje);
	}
}
