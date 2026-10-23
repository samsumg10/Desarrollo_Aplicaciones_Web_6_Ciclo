package com.teleticket.ordenes.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.teleticket.ordenes.client.dto.CrearReservaRequest;
import com.teleticket.ordenes.client.dto.ReservaResponse;

@FeignClient(name = "ms-inventario", url = "${servicios.inventario.url}")
public interface InventarioClient {

	@PostMapping("/internal/reservas")
	ReservaResponse reservar(@RequestBody CrearReservaRequest req);

	@PutMapping("/internal/reservas/{reservaId}/en-pago")
	ReservaResponse enPago(@PathVariable("reservaId") UUID reservaId);

	@PutMapping("/internal/reservas/{reservaId}/activar")
	ReservaResponse activar(@PathVariable("reservaId") UUID reservaId);
}
