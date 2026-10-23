package com.teleticket.mensajeria.mensajes;

import java.util.UUID;

public record OrdenExpirada(
		UUID ordenId,
		UUID usuarioId,
		UUID eventoId) {
}
