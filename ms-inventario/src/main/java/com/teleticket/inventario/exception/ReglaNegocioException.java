package com.teleticket.inventario.exception;

/**
 * Se intento algo que las reglas del negocio no permiten.
 *
 * Ejemplos en esta fase:
 *   - reservar mas entradas de las que quedan (SIN_STOCK)
 *   - pedir una segunda reserva para una orden con otra zona o cantidad
 *   - mover una reserva a un estado que no le corresponde
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
