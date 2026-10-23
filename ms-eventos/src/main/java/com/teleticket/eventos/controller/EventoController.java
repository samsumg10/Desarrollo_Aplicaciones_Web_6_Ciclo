package com.teleticket.eventos.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.teleticket.eventos.dto.CrearEventoRequest;
import com.teleticket.eventos.dto.CrearZonaRequest;
import com.teleticket.eventos.dto.EventoResponse;
import com.teleticket.eventos.dto.ZonaResponse;
import com.teleticket.eventos.service.EventoService;

import jakarta.validation.Valid;

/**
 * API publica del catalogo de eventos.
 *
 * Se llega aqui a traves del API Gateway: el cliente pide a
 * http://localhost:8090/api/eventos y el Gateway lo reenvia al 8082.
 *
 * Observa que la ruta base es /api/eventos y NO solo /eventos: el Gateway
 * reenvia la ruta completa sin recortar el prefijo, asi que el controlador
 * debe responder en la misma direccion que anuncia el Gateway.
 *
 * Casos de uso: CU-02 (consultar), CU-14 (configurar), CU-15 (publicar).
 */
@RestController
@RequestMapping("/api/eventos")
public class EventoController {

	private final EventoService servicio;

	public EventoController(EventoService servicio) {
		this.servicio = servicio;
	}

	// =================================================================
	// CU-02: consultar eventos  (visitante y comprador)
	// =================================================================

	/**
	 * Lista los eventos.
	 *
	 * Por defecto solo los EN_VENTA, que es lo que ve el publico. Con
	 * ?incluirBorradores=true los ve todos, para el operador.
	 *
	 * Cuando llegue Spring Security, ese parametro se reemplazara por una
	 * comprobacion del rol OPERADOR.
	 */
	@GetMapping
	public List<EventoResponse> listar(
			@RequestParam(defaultValue = "false") boolean incluirBorradores) {
		return incluirBorradores ? servicio.listarTodos() : servicio.listarPublicados();
	}

	/** Un evento con sus zonas, precios, hora de venta, reglas y condiciones. */
	@GetMapping("/{eventoId}")
	public EventoResponse verDetalle(@PathVariable UUID eventoId) {
		return servicio.verDetalle(eventoId);
	}

	// =================================================================
	// CU-14: configurar evento, zonas y aforos  (operador)
	// =================================================================

	/**
	 * Crea un evento en estado BORRADOR.
	 *
	 * Devuelve 201 Created con la cabecera Location, que es la convencion REST
	 * para indicar donde quedo el recurso nuevo.
	 *
	 * @Valid activa las validaciones del DTO. Si fallan, ManejadorErrores
	 *        responde 400 con la lista de campos incorrectos.
	 */
	@PostMapping
	public ResponseEntity<EventoResponse> crear(@Valid @RequestBody CrearEventoRequest req) {
		EventoResponse creado = servicio.crear(req);
		return ResponseEntity
				.created(URI.create("/api/eventos/" + creado.id()))
				.body(creado);
	}

	/** Agrega una zona con su precio y su aforo. Solo si el evento sigue en BORRADOR. */
	@PostMapping("/{eventoId}/zonas")
	public ResponseEntity<ZonaResponse> agregarZona(
			@PathVariable UUID eventoId,
			@Valid @RequestBody CrearZonaRequest req) {
		ZonaResponse zona = servicio.agregarZona(eventoId, req);
		return ResponseEntity
				.created(URI.create("/api/eventos/" + eventoId + "/zonas/" + zona.id()))
				.body(zona);
	}

	/**
	 * Modifica un evento en BORRADOR.
	 *
	 * Si el evento ya esta EN_VENTA responde 409: la hora y las reglas se
	 * publicaron al comprador y cambiarlas seria repetir el caso de 2017 (RN-01).
	 */
	@PutMapping("/{eventoId}")
	public EventoResponse actualizar(
			@PathVariable UUID eventoId,
			@Valid @RequestBody CrearEventoRequest req) {
		return servicio.actualizar(eventoId, req);
	}

	// =================================================================
	// CU-15: publicar reglas y horario  (operador)
	// =================================================================

	/**
	 * Publica el evento: pasa a EN_VENTA y queda congelado.
	 *
	 * Tambien publica el mensaje evento.publicado, que hace que ms-cola prepare
	 * la sala de espera y ms-inventario cree el stock de cada zona. Si RabbitMQ
	 * no responde, devuelve 503 y el evento sigue en BORRADOR.
	 *
	 * El X-Request-Id lo pone el Gateway; llamando directo al puerto 8082 puede
	 * faltar y el mensaje llevara un correlationId nuevo.
	 */
	@PutMapping("/{eventoId}/publicar")
	public EventoResponse publicar(@PathVariable UUID eventoId,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		return servicio.publicar(eventoId, requestId);
	}
}
