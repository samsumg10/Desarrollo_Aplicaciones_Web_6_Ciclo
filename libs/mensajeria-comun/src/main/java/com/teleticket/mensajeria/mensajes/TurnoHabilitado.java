package com.teleticket.mensajeria.mensajes;

import java.time.Instant;
import java.util.UUID;

public record TurnoHabilitado(
		UUID turnoId,
		UUID usuarioId,
		UUID eventoId,
		String paseCodigo,
		Instant paseVenceEn) {
}
