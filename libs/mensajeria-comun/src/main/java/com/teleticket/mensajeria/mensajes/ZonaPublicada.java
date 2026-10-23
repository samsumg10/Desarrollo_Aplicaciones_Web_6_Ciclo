package com.teleticket.mensajeria.mensajes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Una zona dentro del mensaje evento.publicado.
 *
 * 'aforo' es el total de la zona: ms-inventario lo convierte en el stock
 * inicial (disponible = aforo).
 */
public record ZonaPublicada(
		UUID id,
		String nombre,
		BigDecimal precio,
		Integer aforo) {
}
