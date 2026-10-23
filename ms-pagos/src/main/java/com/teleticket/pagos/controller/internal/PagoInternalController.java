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

@RestController
@RequestMapping("/internal")
public class PagoInternalController {

	private final PagoService servicio;

	public PagoInternalController(PagoService servicio) {
		this.servicio = servicio;
	}

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
