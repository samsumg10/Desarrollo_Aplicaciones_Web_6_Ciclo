package com.teleticket.mensajeria.mensajes;

import java.math.BigDecimal;
import java.util.UUID;

public record OrdenPagada(
		UUID ordenId,
		UUID usuarioId,
		UUID eventoId,
		UUID zonaId,
		Integer cantidad,
		UUID reservaId,
		BigDecimal total) {
}
