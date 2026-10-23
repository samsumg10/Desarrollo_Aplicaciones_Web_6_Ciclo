package com.teleticket.mensajeria.mensajes;

import java.math.BigDecimal;
import java.util.UUID;

public record PagoReembolsado(
		UUID ordenId,
		BigDecimal monto,
		String motivo) {
}
