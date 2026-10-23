package com.teleticket.eventos.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.teleticket.eventos.entity.Evento;

public record EventoResponse(
		UUID id,
		String nombre,
		Instant fecha,
		String recinto,
		Instant inicioVenta,
		String estado,
		Integer maxPorCliente,
		String condicionesIngreso,
		String reglasCola,
		Integer aforoTotal,
		Instant creadoEn,
		Instant publicadoEn,
		List<ZonaResponse> zonas) {

	public static EventoResponse de(Evento e) {
		return new EventoResponse(
				e.getId(),
				e.getNombre(),
				e.getFecha(),
				e.getRecinto(),
				e.getInicioVenta(),
				e.getEstado().name(),
				e.getMaxPorCliente(),
				e.getCondicionesIngreso(),
				e.getReglasCola(),
				e.aforoTotal(),
				e.getCreadoEn(),
				e.getPublicadoEn(),
				e.getZonas().stream().map(ZonaResponse::de).toList());
	}
}
