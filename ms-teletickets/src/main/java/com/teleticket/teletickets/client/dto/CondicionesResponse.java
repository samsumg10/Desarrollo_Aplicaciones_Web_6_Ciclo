package com.teleticket.teletickets.client.dto;

import java.time.Instant;
import java.util.UUID;

public record CondicionesResponse(
		UUID eventoId,
		String nombreEvento,
		Instant fechaEvento,
		String recinto,
		String condicionesIngreso) {
}
