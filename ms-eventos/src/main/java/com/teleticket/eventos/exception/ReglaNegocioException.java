package com.teleticket.eventos.exception;

/**
 * Se intento algo que las reglas del negocio no permiten.
 *
 * Ejemplos en esta fase:
 *   - cambiar la hora de venta de un evento ya publicado (RN-01)
 *   - crear dos zonas con el mismo nombre en un evento
 *   - publicar un evento que no tiene ninguna zona
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
