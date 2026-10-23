package com.teleticket.ordenes.exception;

public class ServicioNoDisponibleException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final String servicio;

	public ServicioNoDisponibleException(String servicio, String detalle, Throwable causa) {
		super(servicio + " no disponible: " + detalle, causa);
		this.servicio = servicio;
	}

	public String getServicio() {
		return servicio;
	}
}
