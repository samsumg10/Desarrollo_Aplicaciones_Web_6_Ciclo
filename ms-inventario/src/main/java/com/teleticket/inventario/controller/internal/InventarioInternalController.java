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

/**
 * API INTERNA de ms-inventario: solo la llama ms-ordenes (por Feign, fase 8).
 *
 * ms-inventario no tiene API publica (seccion 8.4). Estas rutas no estan en el
 * API Gateway: solo se alcanzan por el puerto directo (8084). Esa ausencia de
 * ruta es la medida de seguridad: un comprador no puede reservar saltandose la
 * cola de espera y el pase de compra.
 */
@RestController
@RequestMapping("/internal")
public class InventarioInternalController {

	private final ReservaService servicio;

	public InventarioInternalController(ReservaService servicio) {
		this.servicio = servicio;
	}

	/** Reserva N entradas de una zona. 201 con la reserva ACTIVA, o 409 si no hay stock. */
	@PostMapping("/reservas")
	public ResponseEntity<ReservaResponse> reservar(@Valid @RequestBody CrearReservaRequest req) {
		ReservaResponse reserva = servicio.reservar(req);
		return ResponseEntity
				.created(URI.create("/internal/reservas/" + reserva.id()))
				.body(reserva);
	}

	/** Protege la reserva mientras se cobra, para que no venza a mitad del pago. */
	@PutMapping("/reservas/{reservaId}/en-pago")
	public ReservaResponse enPago(@PathVariable UUID reservaId) {
		return servicio.marcarEnPago(reservaId);
	}

	/** La devuelve a ACTIVA si el pago se rechazo, para que el comprador pueda reintentar. */
	@PutMapping("/reservas/{reservaId}/activar")
	public ReservaResponse activar(@PathVariable UUID reservaId) {
		return servicio.activar(reservaId);
	}

	/** Cuantas entradas quedan en la zona (un numero). */
	@GetMapping("/zonas/{zonaId}/disponible")
	public int disponible(@PathVariable UUID zonaId) {
		return servicio.disponible(zonaId);
	}
}
