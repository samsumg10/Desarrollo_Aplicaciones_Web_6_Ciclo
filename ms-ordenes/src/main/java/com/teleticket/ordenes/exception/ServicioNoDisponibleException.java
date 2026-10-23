package com.teleticket.ordenes.exception;

/**
 * Un servicio del que depende la orden no respondio a tiempo, o fallo. Es 503.
 *
 * No es una regla de negocio ni culpa del comprador: puede reintentar. Los
 * timeouts de Feign (application.properties) garantizan que esto llega en pocos
 * segundos en vez de dejar la peticion colgada.
 */
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
