package com.teleticket.ordenes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.teleticket.ordenes.client.dto.ConsumirPaseRequest;
import com.teleticket.ordenes.client.dto.DevolverPaseRequest;
import com.teleticket.ordenes.client.dto.PaseConsumidoResponse;

/**
 * Llamadas a ms-cola (API interna, puerto directo 8083).
 *
 * Feign convierte esta interfaz en peticiones HTTP: llamar a consumirPase(...) hace
 * un POST a {servicios.cola.url}/internal/pases/{codigo}/consumir. Los errores de
 * negocio de ms-cola (409) los convierte ClienteErrorDecoder en excepciones propias.
 */
@FeignClient(name = "ms-cola", url = "${servicios.cola.url}")
public interface ColaClient {

	/** Consume el pase (un pase, una compra) exigiendo que sea de esa persona y de ese evento. */
	@PostMapping("/internal/pases/{codigo}/consumir")
	PaseConsumidoResponse consumirPase(@PathVariable("codigo") String codigo, @RequestBody ConsumirPaseRequest req);

	/** Devuelve un pase consumido: la compensacion si no se pudo completar la orden. */
	@PostMapping("/internal/pases/{codigo}/devolver")
	PaseConsumidoResponse devolverPase(@PathVariable("codigo") String codigo, @RequestBody DevolverPaseRequest req);
}
