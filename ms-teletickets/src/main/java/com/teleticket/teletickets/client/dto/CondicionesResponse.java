package com.teleticket.teletickets.client.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Lo que responde ms-eventos en GET /internal/eventos/{id}/condiciones.
 */
public record CondicionesResponse(
		UUID eventoId,
		String nombreEvento,
		Instant fechaEvento,
		String recinto,
		String condicionesIngreso) {
}
