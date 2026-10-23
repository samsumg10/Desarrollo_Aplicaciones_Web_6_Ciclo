package com.teleticket.cola.dto;

import java.util.UUID;

import com.teleticket.cola.entity.PaseCompra;

public record PaseConsumidoResponse(
		UUID codigo,
		UUID turnoId,
		UUID usuarioId,
		UUID eventoId,
		String estado) {

	public static PaseConsumidoResponse de(PaseCompra p) {
		return new PaseConsumidoResponse(p.getCodigo(), p.getTurnoId(), p.getUsuarioId(),
				p.getEventoId(), p.getEstado().name());
	}
}
