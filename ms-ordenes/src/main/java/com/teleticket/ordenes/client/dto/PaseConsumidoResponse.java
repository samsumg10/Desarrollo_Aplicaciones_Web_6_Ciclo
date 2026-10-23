package com.teleticket.ordenes.client.dto;

import java.util.UUID;

/** Respuesta de ms-cola al consumir o devolver un pase. */
public record PaseConsumidoResponse(
		UUID codigo,
		UUID turnoId,
		UUID usuarioId,
		UUID eventoId,
		String estado) {
}
