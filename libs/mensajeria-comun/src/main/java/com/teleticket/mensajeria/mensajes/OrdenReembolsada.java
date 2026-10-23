package com.teleticket.mensajeria.mensajes;

import java.util.UUID;

public record OrdenReembolsada(
		UUID ordenId,
		UUID usuarioId,
		UUID eventoId,
		UUID zonaId,
		Integer cantidad,
		UUID reservaId) {
}
