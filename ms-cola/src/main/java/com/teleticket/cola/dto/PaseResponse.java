package com.teleticket.cola.dto;

import java.time.Instant;
import java.util.UUID;

import com.teleticket.cola.entity.PaseCompra;

/**
 * El pase de compra tal como lo ve su duenio. El 'codigo' es lo que el comprador
 * envia en la cabecera X-Pase-Compra al crear la orden (fase 8).
 */
public record PaseResponse(
		UUID codigo,
		Instant expiraEn,
		String estado) {

	public static PaseResponse de(PaseCompra p) {
		return new PaseResponse(p.getCodigo(), p.getExpiraEn(), p.getEstado().name());
	}
}
