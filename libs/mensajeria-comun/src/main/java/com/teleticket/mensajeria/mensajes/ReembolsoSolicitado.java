package com.teleticket.mensajeria.mensajes;

import java.math.BigDecimal;
import java.util.UUID;

public record ReembolsoSolicitado(
		UUID ordenId,
		BigDecimal monto,
		String motivo) {
}
