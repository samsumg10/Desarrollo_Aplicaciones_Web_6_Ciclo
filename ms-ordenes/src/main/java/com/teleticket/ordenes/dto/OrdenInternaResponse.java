package com.teleticket.ordenes.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.teleticket.ordenes.entity.Orden;
import com.teleticket.ordenes.entity.OrdenHistorial;

/**
 * La orden completa, para otros servicios (ms-reclamos, fase 17): incluye al comprador,
 * el pase, la reserva y todo el historial de estados.
 */
public record OrdenInternaResponse(
		UUID id,
		String codigo,
		UUID usuarioId,
		UUID eventoId,
		UUID zonaId,
		Integer cantidad,
		BigDecimal precioUnitario,
		BigDecimal total,
		String estado,
		UUID paseCodigo,
		UUID reservaId,
		Instant venceEn,
		Instant creadaEn,
		List<Cambio> historial) {

	/** Una fila del historial. */
	public record Cambio(String estadoAnterior, String estadoNuevo, String motivo, Instant fecha) {

		static Cambio de(OrdenHistorial h) {
			return new Cambio(h.getEstadoAnterior() == null ? null : h.getEstadoAnterior().name(),
					h.getEstadoNuevo().name(), h.getMotivo(), h.getFecha());
		}
	}

	public static OrdenInternaResponse de(Orden o, List<OrdenHistorial> historial) {
		return new OrdenInternaResponse(o.getId(), o.getCodigo(), o.getUsuarioId(), o.getEventoId(), o.getZonaId(),
				o.getCantidad(), o.getPrecioUnitario(), o.getTotal(), o.getEstado().name(), o.getPaseCodigo(),
				o.getReservaId(), o.getVenceEn(), o.getCreadaEn(),
				historial.stream().map(Cambio::de).toList());
	}
}
