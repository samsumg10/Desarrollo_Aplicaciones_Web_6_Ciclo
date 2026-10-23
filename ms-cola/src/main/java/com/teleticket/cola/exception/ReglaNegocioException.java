package com.teleticket.cola.exception;

/**
 * Se intento algo que las reglas del negocio no permiten.
 *
 * Ejemplo en esta fase: consumir un pase invalido, vencido, ajeno o ya usado
 * (PASE_NO_VALIDO).
 *
 * El manejador global la traduce a HTTP 409 (Conflict). Lleva un 'codigo'
 * corto y estable para que el cliente pueda reaccionar sin leer el texto.
 */
public class ReglaNegocioException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final String codigo;

	public ReglaNegocioException(String codigo, String mensaje) {
		super(mensaje);
		this.codigo = codigo;
	}

	public String getCodigo() {
		return codigo;
	}
}
