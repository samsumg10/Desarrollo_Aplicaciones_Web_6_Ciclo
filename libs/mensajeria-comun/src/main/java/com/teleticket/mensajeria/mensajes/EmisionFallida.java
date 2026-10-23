package com.teleticket.mensajeria.mensajes;

import java.util.UUID;

public record EmisionFallida(
		UUID ordenId,
		String motivo) {
}
