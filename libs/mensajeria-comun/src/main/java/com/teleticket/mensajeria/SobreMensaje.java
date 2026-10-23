package com.teleticket.mensajeria;

import java.time.Instant;
import java.util.UUID;

public record SobreMensaje<T>(
		UUID eventoId,
		String tipo,
		Instant fecha,
		String correlationId,
		T datos) {

	public static <T> SobreMensaje<T> de(String tipo, String correlationId, T datos) {
		return new SobreMensaje<>(
				UUID.randomUUID(),
				tipo,
				Instant.now(),
				correlationId != null && !correlationId.isBlank() ? correlationId : UUID.randomUUID().toString(),
				datos);
	}
}
