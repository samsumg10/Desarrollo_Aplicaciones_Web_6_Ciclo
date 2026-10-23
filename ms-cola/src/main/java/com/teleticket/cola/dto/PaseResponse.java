package com.teleticket.cola.dto;

import java.time.Instant;
import java.util.UUID;

import com.teleticket.cola.entity.PaseCompra;

public record PaseResponse(
		UUID codigo,
		Instant expiraEn,
		String estado) {

	public static PaseResponse de(PaseCompra p) {
		return new PaseResponse(p.getCodigo(), p.getExpiraEn(), p.getEstado().name());
	}
}
