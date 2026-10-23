package com.teleticket.ordenes.client.dto;

import java.time.Instant;
import java.util.UUID;

public record ReservaResponse(
		UUID id,
		UUID ordenId,
		UUID zonaId,
		Integer cantidad,
		String estado,
		Instant venceEn) {
}
