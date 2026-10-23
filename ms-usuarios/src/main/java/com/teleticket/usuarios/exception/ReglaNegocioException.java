package com.teleticket.usuarios.exception;

/** Una regla del negocio lo prohibe -> HTTP 409. Lleva un codigo estable para el cliente. */
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
