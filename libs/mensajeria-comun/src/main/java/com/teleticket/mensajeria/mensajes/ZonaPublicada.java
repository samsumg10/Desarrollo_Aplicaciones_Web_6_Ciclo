package com.teleticket.mensajeria.mensajes;

import java.math.BigDecimal;
import java.util.UUID;

public record ZonaPublicada(
		UUID id,
		String nombre,
		BigDecimal precio,
		Integer aforo) {
}
