package com.teleticket.ordenes.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CobrarRequest(
		UUID ordenId,
		BigDecimal monto,
		String numeroTarjeta,
		String titular) {
}
