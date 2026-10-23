package com.teleticket.ordenes.client.dto;

import java.util.UUID;

public record PaseConsumidoResponse(
		UUID codigo,
		UUID turnoId,
		UUID usuarioId,
		UUID eventoId,
		String estado) {
}
