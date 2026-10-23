package com.teleticket.ordenes.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

	/**
	 * ACTIVA -> EN_PAGO: protege la reserva mientras se cobra. Rechaza (409 RESERVA_VENCIDA) las
	 * ya vencidas. Si ya esta EN_PAGO responde OK, asi que se puede repetir.
	 */
	@PutMapping("/internal/reservas/{reservaId}/en-pago")
	ReservaResponse enPago(@PathVariable("reservaId") UUID reservaId);

	/** EN_PAGO -> ACTIVA: el pago se rechazo y el comprador puede reintentar. Tambien repetible. */
	@PutMapping("/internal/reservas/{reservaId}/activar")
	ReservaResponse activar(@PathVariable("reservaId") UUID reservaId);
}
