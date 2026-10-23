package com.teleticket.ordenes.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ZonaInternaResponse(
		UUID zonaId,
		UUID eventoId,
		String nombreZona,
		BigDecimal precio,
		Integer aforo,
		Integer maxPorCliente,
		boolean ventaAbierta) {
}
