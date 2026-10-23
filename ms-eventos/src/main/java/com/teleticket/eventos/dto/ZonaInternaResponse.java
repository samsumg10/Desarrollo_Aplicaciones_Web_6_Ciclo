package com.teleticket.eventos.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lo que ms-ordenes necesita saber para crear una orden (fase 8).
 *
 * Lo pide por Feign a GET /internal/eventos/{id}/zonas/{zonaId}, y con esto
 * calcula el total y valida el maximo por comprador (RN-05).
 *
 * Es una respuesta distinta de ZonaResponse a proposito: aqui viajan datos que
 * el publico no necesita (maxPorCliente, ventaAbierta) y se omiten los que no
 * hacen falta. Cada consumidor recibe exactamente lo que usa.
 */
public record ZonaInternaResponse(
		UUID zonaId,
		UUID eventoId,
		String nombreZona,
		BigDecimal precio,
		Integer aforo,

		/** Maximo de entradas por comprador y evento (RN-05). */
		Integer maxPorCliente,

		/**
		 * True si el evento esta EN_VENTA y ya paso la hora de inicio.
		 *
		 * ms-ordenes rechaza la orden si esto es false: asi nadie compra antes
		 * de la hora anunciada, que es parte de PRB-02.
		 */
		boolean ventaAbierta) {
}
