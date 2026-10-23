package com.teleticket.eventos.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.teleticket.eventos.entity.Zona;

public record ZonaResponse(
		UUID id,
		String nombre,
		BigDecimal precio,
		Integer aforo) {

	public static ZonaResponse de(Zona z) {
		return new ZonaResponse(z.getId(), z.getNombre(), z.getPrecio(), z.getAforo());
	}
}
