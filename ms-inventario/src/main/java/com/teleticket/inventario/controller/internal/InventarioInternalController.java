package com.teleticket.inventario.controller.internal;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teleticket.inventario.dto.CrearReservaRequest;
import com.teleticket.inventario.dto.ReservaResponse;
import com.teleticket.inventario.service.ReservaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/internal")
public class InventarioInternalController {

	private final ReservaService servicio;

	public InventarioInternalController(ReservaService servicio) {
		this.servicio = servicio;
	}

	@PostMapping("/reservas")
	public ResponseEntity<ReservaResponse> reservar(@Valid @RequestBody CrearReservaRequest req) {
		ReservaResponse reserva = servicio.reservar(req);
		return ResponseEntity
				.created(URI.create("/internal/reservas/" + reserva.id()))
				.body(reserva);
	}

	@PutMapping("/reservas/{reservaId}/en-pago")
	public ReservaResponse enPago(@PathVariable UUID reservaId) {
		return servicio.marcarEnPago(reservaId);
	}

	@PutMapping("/reservas/{reservaId}/activar")
	public ReservaResponse activar(@PathVariable UUID reservaId) {
		return servicio.activar(reservaId);
	}

	@GetMapping("/zonas/{zonaId}/disponible")
	public int disponible(@PathVariable UUID zonaId) {
		return servicio.disponible(zonaId);
	}
}
