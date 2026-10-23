package com.teleticket.pagos.controller.internal;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teleticket.pagos.dto.CobrarRequest;
import com.teleticket.pagos.dto.PagoResponse;
import com.teleticket.pagos.dto.ResultadoCobro;
import com.teleticket.pagos.service.PagoService;

import jakarta.validation.Valid;

/**
 * API INTERNA de ms-pagos: solo la llama ms-ordenes (por Feign).
 *
 * ms-pagos no tiene API publica (seccion 8.4). Estas rutas no estan en el API Gateway:
 * solo se alcanzan por el puerto directo (8086). Un comprador no puede pedir un cobro
 * por su cuenta: tiene que pasar por POST /api/ordenes/{id}/pagar.
 */
@RestController
@RequestMapping("/internal")
public class PagoInternalController {

	private final PagoService servicio;

	public PagoInternalController(PagoService servicio) {
		this.servicio = servicio;
	}

	/**
	 * Cobra una orden. Idempotente: cobrarla otra vez devuelve el mismo pago.
	 *
	 * 201 si esta peticion paso por la pasarela, 200 si se devolvio un pago que ya existia.
	 * Un pago RECHAZADO tambien es 201/200: la pasarela dijo "no", pero la peticion se atendio bien.
	 * 409 si el pago se esta procesando (PAGO_EN_CURSO) o la orden ya tiene un pago por otro monto.
	 */
	@PostMapping("/pagos")
	public ResponseEntity<PagoResponse> cobrar(@Valid @RequestBody CobrarRequest req) {
		ResultadoCobro resultado = servicio.cobrar(req);
		PagoResponse pago = resultado.pago();
		if (resultado.nuevo()) {
			return ResponseEntity.created(URI.create("/internal/pagos/" + pago.id())).body(pago);
		}
		return ResponseEntity.ok(pago);
	}
}
