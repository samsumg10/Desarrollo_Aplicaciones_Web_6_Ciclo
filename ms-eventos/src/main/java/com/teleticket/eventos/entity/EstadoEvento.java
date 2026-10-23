package com.teleticket.eventos.entity;

/**
 * Ciclo de vida de un evento.
 *
 * El salto clave es BORRADOR -> EN_VENTA: al publicarlo, la hora de venta y
 * las reglas quedan congeladas para siempre (RN-01). Eso es lo que impide
 * repetir el incidente de 2017, cuando la cola se anuncio para las 6:00 a. m.
 * y el sistema reordeno a todos desde las 5:00.
 */
public enum EstadoEvento {

	/** Se esta configurando. Todo es editable y no se ve en el catalogo publico. */
	BORRADOR,

	/** Publicado. La hora de venta, las reglas y las condiciones ya NO se pueden cambiar. */
	EN_VENTA,

	/** El evento ya ocurrio. */
	FINALIZADO,

	/** Cancelado. Dispara reembolsos a todos los compradores (RN-13). */
	CANCELADO
}
