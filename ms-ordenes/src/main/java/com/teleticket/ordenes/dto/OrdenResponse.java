package com.teleticket.ordenes.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.teleticket.ordenes.entity.Orden;

/**
 * La orden tal como la ve su comprador.
 *
 * No incluye el pase ni la reserva: el pase ya esta gastado y la reserva es un detalle
 * interno entre servicios.
 *
 * 'segundosParaPagar' es lo que le queda al comprador hasta 'venceEn' (nunca negativo).
 */
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
