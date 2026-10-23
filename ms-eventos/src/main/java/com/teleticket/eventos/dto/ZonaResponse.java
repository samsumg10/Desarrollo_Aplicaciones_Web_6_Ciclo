package com.teleticket.eventos.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.teleticket.eventos.entity.Zona;

/**
 * Una zona tal como la ve el cliente.
 *
 * Observa que NO incluye cuantas entradas quedan: eso lo lleva ms-inventario,
 * que es el dueno de ese dato. ms-eventos solo conoce el aforo total. Es el
 * patron 'database per service' en accion: cada servicio responde solo por lo
 * suyo (seccion 7 del documento de arquitectura).
 */
public record ZonaResponse(
		UUID id,
		String nombre,
		BigDecimal precio,
		Integer aforo) {

	public static ZonaResponse de(Zona z) {
		return new ZonaResponse(z.getId(), z.getNombre(), z.getPrecio(), z.getAforo());
	}
}
