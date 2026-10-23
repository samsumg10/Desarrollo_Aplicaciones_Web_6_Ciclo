package com.teleticket.ordenes.controller.internal;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teleticket.ordenes.dto.OrdenInternaResponse;
import com.teleticket.ordenes.service.OrdenService;

/**
 * API INTERNA de ms-ordenes: solo la llaman otros servicios (ms-reclamos, fase 17).
 *
 * No tiene ruta en el API Gateway: solo se alcanza por el puerto directo (8085). Devuelve
 * la orden de cualquier persona con todo su historial, algo que no debe ver el publico.
 */
@RestController
@RequestMapping("/internal/ordenes")
public class OrdenInternalController {

	private final OrdenService servicio;

	public OrdenInternalController(OrdenService servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/{ordenId}")
	public OrdenInternaResponse ver(@PathVariable UUID ordenId) {
		return servicio.interna(ordenId);
	}
}
