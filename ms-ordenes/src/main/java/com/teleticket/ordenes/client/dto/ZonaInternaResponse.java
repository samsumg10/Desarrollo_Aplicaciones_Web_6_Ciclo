package com.teleticket.ordenes.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Respuesta de ms-eventos con lo que necesita una orden: precio, maximo por comprador
 * y si la venta ya esta abierta. Es una copia del contrato, no una clase compartida:
 * cada servicio es dueno del suyo y Jackson ignora los campos que sobren.
 */
public record ZonaInternaResponse(
		UUID zonaId,
		UUID eventoId,
		String nombreZona,
		BigDecimal precio,
		Integer aforo,
		Integer maxPorCliente,
		boolean ventaAbierta) {
}
