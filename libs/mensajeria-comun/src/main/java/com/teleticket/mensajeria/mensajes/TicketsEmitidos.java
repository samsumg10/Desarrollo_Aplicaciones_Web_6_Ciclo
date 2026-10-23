package com.teleticket.mensajeria.mensajes;

import java.util.List;
import java.util.UUID;

public record TicketsEmitidos(
		UUID ordenId,
		UUID usuarioId,
		UUID eventoId,
		List<UUID> ticketIds) {
}
