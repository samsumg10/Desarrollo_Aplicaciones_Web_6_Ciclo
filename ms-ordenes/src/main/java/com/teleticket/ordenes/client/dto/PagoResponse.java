package com.teleticket.ordenes.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Respuesta de ms-pagos. 'estado' es APROBADO o RECHAZADO; 'motivoRechazo' solo viene si fue rechazado. */
public record PagoResponse(
		UUID id,
		UUID ordenId,
		BigDecimal monto,
		String estado,
		String referenciaPasarela,
		String motivoRechazo) {
}
