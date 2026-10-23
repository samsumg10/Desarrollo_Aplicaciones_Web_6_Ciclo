package com.teleticket.ordenes.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Lo que se envia a ms-pagos para cobrar. La clave de idempotencia es el ordenId. */
public record CobrarRequest(
		UUID ordenId,
		BigDecimal monto,
		String numeroTarjeta,
		String titular) {
}
