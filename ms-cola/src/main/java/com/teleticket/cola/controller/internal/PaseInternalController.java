package com.teleticket.cola.controller.internal;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teleticket.cola.dto.ConsumirPaseRequest;
import com.teleticket.cola.dto.PaseConsumidoResponse;
import com.teleticket.cola.service.PaseService;

import jakarta.validation.Valid;

/**
 * API INTERNA de ms-cola: solo la llama ms-ordenes (por Feign, fase 8).
 *
 * No tiene ruta en el API Gateway: solo se alcanza por el puerto directo (8083).
 * Esa ausencia es la medida de seguridad: un comprador no puede consumir pases.
 */
@RestController
@RequestMapping("/internal")
public class PaseInternalController {

	private final PaseService servicio;

	public PaseInternalController(PaseService servicio) {
		this.servicio = servicio;
	}

	/**
	 * Consume el pase. 200 la primera vez; 409 PASE_NO_VALIDO si no existe, es ajeno,
	 * ya se uso o vencio.
	 */
	@PostMapping("/pases/{codigo}/consumir")
	public PaseConsumidoResponse consumir(@PathVariable UUID codigo,
			@Valid @RequestBody ConsumirPaseRequest req) {
		return servicio.consumir(codigo, req.usuarioId());
	}
}
