package com.teleticket.ordenes.client.dto;

import java.time.Instant;
import java.util.UUID;

/** Respuesta de ms-inventario al reservar. 'venceEn' es hasta cuando puede pagar el comprador. */
public record ReservaResponse(
		UUID id,
		UUID ordenId,
		UUID zonaId,
		Integer cantidad,
		String estado,
		Instant venceEn) {
}
