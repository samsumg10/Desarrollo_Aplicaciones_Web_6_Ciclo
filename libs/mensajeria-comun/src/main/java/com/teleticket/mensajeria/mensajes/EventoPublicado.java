package com.teleticket.mensajeria.mensajes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventoPublicado(
		UUID id,
		String nombre,
		Instant fecha,
		String recinto,
		Instant inicioVenta,
		Integer maxPorCliente,
		String reglasCola,
		List<ZonaPublicada> zonas) {
}
