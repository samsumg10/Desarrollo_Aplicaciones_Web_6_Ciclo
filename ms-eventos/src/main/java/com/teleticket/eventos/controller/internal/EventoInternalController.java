package com.teleticket.eventos.controller.internal;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teleticket.eventos.dto.CondicionesResponse;
import com.teleticket.eventos.dto.ZonaInternaResponse;
import com.teleticket.eventos.service.EventoService;

/**
 * API INTERNA: solo la llaman otros microservicios, nunca el navegador.
 *
 * Por que esta separada del controlador publico:
 *
 *   1. El API Gateway NO tiene ruta hacia /internal/**, asi que desde fuera
 *      del sistema estos endpoints no existen. Compruebalo: por el puerto
 *      directo 8082 responden, y por el Gateway (8090) dan 404.
 *
 *   2. Devuelven datos que el publico no necesita (el maximo por cliente, si
 *      la venta esta abierta) y que no conviene exponer.
 *
 * Esa separacion es parte de la solucion a PRB-03: cuanto menos haya expuesto
 * hacia fuera, menos formas hay de saltarse la cola.
 *
 * Quien llama a cada endpoint:
 *   ms-ordenes  (fase 8)  -> /zonas/{zonaId}   para el precio y el limite
 *   ms-tickets  (fase 10) -> /condiciones      para copiarlas en el e-ticket
 */
@RestController
@RequestMapping("/internal/eventos")
public class EventoInternalController {

	private final EventoService servicio;

	public EventoInternalController(EventoService servicio) {
		this.servicio = servicio;
	}

	/**
	 * Precio, aforo, maximo por cliente y si la venta esta abierta.
	 *
	 * Lo usara ms-ordenes para calcular el total de la orden y validar RN-05.
	 */
	@GetMapping("/{eventoId}/zonas/{zonaId}")
	public ZonaInternaResponse datosDeZona(
			@PathVariable UUID eventoId,
			@PathVariable UUID zonaId) {
		return servicio.datosDeZona(eventoId, zonaId);
	}

	/**
	 * Las condiciones de ingreso del evento.
	 *
	 * ms-tickets copia este texto en cada e-ticket y ms-notificaciones en cada
	 * correo, para que lo informado en la compra sea lo mismo que se valida en
	 * la puerta (PRB-06).
	 */
	@GetMapping("/{eventoId}/condiciones")
	public CondicionesResponse condiciones(@PathVariable UUID eventoId) {
		return servicio.condicionesDeIngreso(eventoId);
	}
}
