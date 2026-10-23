package com.teleticket.inventario.dto;

import java.time.Instant;
import java.util.UUID;

import com.teleticket.inventario.entity.Reserva;

public record ReservaResponse(
		UUID id,
		UUID ordenId,
		UUID zonaId,
		Integer cantidad,
		String estado,
		Instant venceEn) {

	public static ReservaResponse de(Reserva r) {
		return new ReservaResponse(r.getId(), r.getOrdenId(), r.getZonaId(),
				r.getCantidad(), r.getEstado().name(), r.getVenceEn());
	}
}
