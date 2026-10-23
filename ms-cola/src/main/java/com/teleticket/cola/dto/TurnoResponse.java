package com.teleticket.cola.dto;

import java.time.Instant;
import java.util.UUID;

public record TurnoResponse(
		UUID turnoId,
		UUID eventoId,
		String estado,
		Integer posicion,
		Long turnosDelante,
		Long esperaEstimadaSegundos,
		Instant inicioVenta,
		PaseResponse pase) {
}
