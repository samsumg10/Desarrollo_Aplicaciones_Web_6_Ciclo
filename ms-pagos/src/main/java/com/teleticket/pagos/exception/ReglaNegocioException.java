package com.teleticket.pagos.exception;

/**
 * Se intento algo que las reglas del negocio no permiten.
 *
 * Ejemplos: cobrar una orden que ya se esta cobrando (PAGO_EN_CURSO) o con otro monto
 * del que ya tiene (PAGO_CONFLICTO).
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
