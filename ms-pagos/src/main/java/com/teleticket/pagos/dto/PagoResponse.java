package com.teleticket.pagos.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.teleticket.pagos.entity.Pago;

public record PagoResponse(
		UUID id,
		UUID ordenId,
		BigDecimal monto,
		String estado,
		String referenciaPasarela,
		String motivoRechazo,
		String tarjetaUltimos4,
		Integer intentos,
		Instant actualizadoEn) {

	public static PagoResponse de(Pago p) {
		return new PagoResponse(p.getId(), p.getOrdenId(), p.getMonto(), p.getEstado().name(),
				p.getReferenciaPasarela(), p.getMotivoRechazo(), p.getTarjetaUltimos4(), p.getIntentos(),
				p.getActualizadoEn());
	}
}
