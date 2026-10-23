package com.teleticket.eventos.dto;

import java.time.Instant;
import java.util.UUID;

public record CondicionesResponse(
		UUID eventoId,
		String nombreEvento,
		Instant fechaEvento,
		String recinto,
		String condicionesIngreso) {
}
