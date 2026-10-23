package com.teleticket.ordenes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.teleticket.ordenes.client.dto.CrearReservaRequest;
import com.teleticket.ordenes.client.dto.ReservaResponse;

/**
 * Llamadas a ms-inventario (API interna, puerto directo 8084).
 */
@FeignClient(name = "ms-inventario", url = "${servicios.inventario.url}")
public interface InventarioClient {

	/**
	 * Reserva N entradas de una zona. Es idempotente por orden: repetirla con los mismos
	 * datos devuelve la misma reserva sin descontar stock otra vez.
	 */
	@PostMapping("/internal/reservas")
	ReservaResponse reservar(@RequestBody CrearReservaRequest req);
}
