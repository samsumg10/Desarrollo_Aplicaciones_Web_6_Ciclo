package com.teleticket.ordenes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.teleticket.ordenes.client.dto.CobrarRequest;
import com.teleticket.ordenes.client.dto.PagoResponse;

/**
 * Llamadas a ms-pagos (API interna, puerto directo 8086).
 */
@FeignClient(name = "ms-pagos", url = "${servicios.pagos.url}")
public interface PagoClient {

	/**
	 * Cobra la orden. Es idempotente por orden: repetirla devuelve el pago que ya existe
	 * sin cobrar otra vez, que es lo que permite reintentar si no llego la respuesta.
	 * Un rechazo de la pasarela NO es un error HTTP: llega como un pago en estado RECHAZADO.
	 */
	@PostMapping("/internal/pagos")
	PagoResponse cobrar(@RequestBody CobrarRequest req);
}
