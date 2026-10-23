package com.teleticket.cola.dto;

import java.util.UUID;

import com.teleticket.cola.entity.PaseCompra;

/**
 * Lo que recibe ms-ordenes al consumir un pase. Incluye el evento para que
 * ms-ordenes compruebe que el pase corresponde al evento que se quiere comprar.
 */
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
