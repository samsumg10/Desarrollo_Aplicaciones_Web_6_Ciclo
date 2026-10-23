package com.teleticket.cola.controller.internal;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teleticket.cola.dto.ConsumirPaseRequest;
import com.teleticket.cola.dto.DevolverPaseRequest;
import com.teleticket.cola.dto.PaseConsumidoResponse;
import com.teleticket.cola.service.PaseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/internal")
public class PaseInternalController {

	private final PaseService servicio;

	public PaseInternalController(PaseService servicio) {
		this.servicio = servicio;
	}

	@PostMapping("/pases/{codigo}/consumir")
	public PaseConsumidoResponse consumir(@PathVariable UUID codigo,
			@Valid @RequestBody ConsumirPaseRequest req) {
		return servicio.consumir(codigo, req.usuarioId(), req.eventoId());
	}

	@PostMapping("/pases/{codigo}/devolver")
	public PaseConsumidoResponse devolver(@PathVariable UUID codigo,
			@Valid @RequestBody DevolverPaseRequest req) {
		return servicio.devolver(codigo, req.usuarioId());
	}
}
