package com.teleticket.ordenes.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.teleticket.ordenes.entity.Orden;

public record OrdenResponse(
		UUID id,
		String codigo,
		UUID eventoId,
		UUID zonaId,
		Integer cantidad,
		BigDecimal precioUnitario,
		BigDecimal total,
		String estado,
		Instant venceEn,
		long segundosParaPagar,
		Instant creadaEn) {

	public static OrdenResponse de(Orden o) {
		long restante = Math.max(0, java.time.Duration.between(Instant.now(), o.getVenceEn()).toSeconds());
		return new OrdenResponse(o.getId(), o.getCodigo(), o.getEventoId(), o.getZonaId(), o.getCantidad(),
				o.getPrecioUnitario(), o.getTotal(), o.getEstado().name(), o.getVenceEn(), restante,
				o.getCreadaEn());
	}
}
