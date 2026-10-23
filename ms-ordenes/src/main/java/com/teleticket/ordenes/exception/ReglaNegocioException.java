package com.teleticket.ordenes.exception;

/**
 * Se intento algo que las reglas del negocio no permiten.
 *
 * Ejemplos en esta fase: pedir mas entradas del maximo por comprador (MAXIMO_EXCEDIDO),
 * comprar antes de la hora de venta (VENTA_CERRADA) y los errores de negocio que
 * devuelven los otros servicios (ver ClienteErrorDecoder).
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
