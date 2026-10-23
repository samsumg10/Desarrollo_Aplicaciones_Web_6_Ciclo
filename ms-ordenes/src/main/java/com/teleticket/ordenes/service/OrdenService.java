package com.teleticket.ordenes.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.teleticket.ordenes.client.ColaClient;
import com.teleticket.ordenes.client.EventoClient;
import com.teleticket.mensajeria.Mensajeria;
import com.teleticket.mensajeria.PublicacionFallidaException;
import com.teleticket.mensajeria.PublicadorEventos;
import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.OrdenPagada;
import com.teleticket.ordenes.client.InventarioClient;
import com.teleticket.ordenes.client.PagoClient;
import com.teleticket.ordenes.client.dto.CobrarRequest;
import com.teleticket.ordenes.client.dto.ConsumirPaseRequest;
import com.teleticket.ordenes.client.dto.CrearReservaRequest;
import com.teleticket.ordenes.client.dto.DevolverPaseRequest;
import com.teleticket.ordenes.client.dto.PagoResponse;
import com.teleticket.ordenes.client.dto.ReservaResponse;
import com.teleticket.ordenes.client.dto.ZonaInternaResponse;
import com.teleticket.ordenes.dto.CrearOrdenRequest;
import com.teleticket.ordenes.dto.OrdenInternaResponse;
import com.teleticket.ordenes.dto.OrdenResponse;
import com.teleticket.ordenes.dto.PagarRequest;
import com.teleticket.ordenes.entity.EstadoOrden;
import com.teleticket.ordenes.entity.Orden;
import com.teleticket.ordenes.entity.OrdenHistorial;
import com.teleticket.ordenes.exception.NoEncontradoException;
import com.teleticket.ordenes.exception.PagoNoConfirmadoException;
import com.teleticket.ordenes.exception.PagoRechazadoException;
import com.teleticket.ordenes.exception.PaseNoValidoException;
import com.teleticket.ordenes.exception.PaseRequeridoException;
import com.teleticket.ordenes.exception.ReglaNegocioException;
import com.teleticket.ordenes.exception.ServicioNoDisponibleException;
import com.teleticket.ordenes.repository.OrdenHistorialRepository;
import com.teleticket.ordenes.repository.OrdenRepository;

import feign.FeignException;

/**
 * Crear y consultar ordenes (CU-05).
 *
 * Crear una orden es una orquestacion de TRES servicios, sin transaccion que las abarque:
 * cada uno es dueno de su base de datos y no existe una transaccion distribuida. Por eso
 * el orden de los pasos importa, y cada uno que cambia algo tiene su compensacion.
 *
 *   1. ms-eventos   precio, maximo y venta abierta     solo LEE        nada que deshacer
 *   2. ms-cola      consumir el pase                   CAMBIA (USADO)  se deshace: devolverPase
 *   3. ms-inventario reservar el stock                 CAMBIA          idempotente por orden
 *   4. guardar la orden y su historial                 CAMBIA (local)  una transaccion local
 *
 * La regla que ordena todo: lo que solo lee va primero. Asi un error de negocio (zona
 * inexistente, venta cerrada, maximo excedido) se detecta ANTES de gastar el pase.
 *
 * Compensacion (la "nota honesta" del plan): si despues de consumir el pase algo falla
 * (sin stock, un servicio caido, un error al guardar), se DEVUELVE el pase en ms-cola
 * para que el comprador no pierda su turno. Se descarto la opcion de reservar el stock
 * antes de consumir el pase: sin pase valido cualquiera podria apartar entradas.
 */
@Service
public class OrdenService {

	private static final Logger log = LoggerFactory.getLogger(OrdenService.class);

	private final OrdenRepository ordenRepo;
	private final OrdenHistorialRepository historialRepo;
	private final ColaClient cola;
	private final EventoClient eventos;
	private final InventarioClient inventario;
	private final PagoClient pagos;
	private final PublicadorEventos publicador;
	private final TransactionTemplate tx;

	public OrdenService(OrdenRepository ordenRepo, OrdenHistorialRepository historialRepo, ColaClient cola,
			EventoClient eventos, InventarioClient inventario, PagoClient pagos, PublicadorEventos publicador,
			TransactionTemplate tx) {
		this.ordenRepo = ordenRepo;
		this.historialRepo = historialRepo;
		this.cola = cola;
		this.eventos = eventos;
		this.inventario = inventario;
		this.pagos = pagos;
		this.publicador = publicador;
		this.tx = tx;
	}

	/**
	 * Crea la orden de {@code usuarioId} con el pase indicado.
	 *
	 * No es @Transactional a proposito: mantener abierta una transaccion de base de datos
	 * mientras se esperan tres llamadas HTTP agotaria las conexiones del pool en un pico.
	 * La unica escritura local (orden + historial) va en su propia transaccion corta.
	 */
	public OrdenResponse crear(UUID usuarioId, String paseCabecera, CrearOrdenRequest req) {
		UUID pase = leerPase(paseCabecera);

		// Un pase, una orden. Se comprueba primero, entre otras cosas porque si ya hay una orden
		// con este pase, devolver "el pase" en una compensacion posterior reabriria un pase gastado.
		if (ordenRepo.existsByPaseCodigo(pase)) {
			throw new PaseNoValidoException("Este pase ya se uso para crear una orden");
		}

		// 1. Solo lectura: precio, maximo por comprador y si la venta esta abierta.
		ZonaInternaResponse zona = llamar("ms-eventos", () -> eventos.datosDeZona(req.eventoId(), req.zonaId()));
		if (!zona.ventaAbierta()) {
			throw new ReglaNegocioException("VENTA_CERRADA",
					"La venta de este evento aun no esta abierta o ya termino");
		}
		// RN-05. Como cada persona tiene un solo pase por evento (un turno, un pase), el limite
		// se comprueba contra esta orden: no puede haber otras anteriores del mismo evento.
		if (req.cantidad() > zona.maxPorCliente()) {
			throw new ReglaNegocioException("MAXIMO_EXCEDIDO",
					"Maximo %d entradas por comprador".formatted(zona.maxPorCliente()));
		}

		// 2. Consumir el pase, exigiendo que sea de esta persona Y de este evento.
		//    Si dice PASE_NO_VALIDO, no se consumio nada: se propaga sin mas.
		//    Si el servicio no responde, no se sabe si lo consumio: se intenta devolver.
		try {
			llamar("ms-cola", () -> cola.consumirPase(pase.toString(), new ConsumirPaseRequest(usuarioId, req.eventoId())));
		} catch (ServicioNoDisponibleException e) {
			devolverPase(pase, usuarioId);
			throw e;
		}

		// 3 y 4. Desde aqui el pase esta gastado: cualquier fallo debe devolverlo.
		try {
			// El id de la orden se deriva del pase: reintentar con el mismo pase produce el mismo
			// ordenId, y la reserva de ms-inventario (UNIQUE por orden) devuelve la misma sin
			// descontar stock dos veces. Asi un reintento tras un timeout no deja stock huerfano.
			UUID ordenId = UUID.nameUUIDFromBytes(("orden:" + pase).getBytes(StandardCharsets.UTF_8));
			ReservaResponse reserva = llamar("ms-inventario", () -> inventario
					.reservar(new CrearReservaRequest(ordenId, req.zonaId(), req.cantidad())));
			return guardar(ordenId, usuarioId, req, zona, pase, reserva);
		} catch (PaseNoValidoException e) {
			// Otra peticion con el mismo pase termino antes y su orden ya existe: el pase le pertenece.
			throw e;
		} catch (RuntimeException e) {
			devolverPase(pase, usuarioId);
			throw e;
		}
	}

	/** Orden + primera fila del historial, en una transaccion local corta. */
	private OrdenResponse guardar(UUID ordenId, UUID usuarioId, CrearOrdenRequest req, ZonaInternaResponse zona,
			UUID pase, ReservaResponse reserva) {
		Orden orden = Orden.crear(ordenId, usuarioId, req.eventoId(), req.zonaId(), req.cantidad(), zona.precio(),
				pase, reserva.id(), reserva.venceEn());
		try {
			return tx.execute(s -> {
				ordenRepo.saveAndFlush(orden);
				historialRepo.save(OrdenHistorial.registrar(ordenId, null, EstadoOrden.RESERVADA,
						"Orden creada: pase consumido y stock reservado"));
				return OrdenResponse.de(orden);
			});
		} catch (DataIntegrityViolationException e) {
			if (ordenRepo.existsByPaseCodigo(pase)) {
				throw new PaseNoValidoException("Este pase ya se uso para crear una orden");
			}
			throw e;
		}
	}

	/**
	 * Paga una orden (CU-06). Tampoco es @Transactional: entre medias hay llamadas HTTP.
	 *
	 * <pre>
	 *   1. validar            la orden es mia y esta RESERVADA (o PENDIENTE_PAGO: un reintento)
	 *   2. inventario.enPago  la reserva no puede vencer mientras se cobra   (idempotente)
	 *   3. RESERVADA -> PENDIENTE_PAGO
	 *   4. pagos.cobrar       idempotente por orden
	 *        APROBADO  -> PAGADA y se publica orden.pagada
	 *        RECHAZADO -> inventario.activar + vuelve a RESERVADA, y 402
	 *        sin respuesta -> se queda PENDIENTE_PAGO, y 503
	 * </pre>
	 *
	 * Pagar es seguro de repetir. Por eso una orden que se queda en PENDIENTE_PAGO (ms-pagos no contesto)
	 * se puede volver a pagar: ms-pagos devuelve el cobro que ya hizo en vez de cobrar de nuevo. Y una
	 * orden ya PAGADA responde 200 con la orden, sin llamar a nadie.
	 *
	 * Lo que NO se hace cuando ms-pagos no responde es devolver la orden a RESERVADA: el cobro pudo
	 * haberse hecho, y dejar la orden "sin pagar" con el dinero ya tomado seria peor que dejarla pendiente.
	 */
	public OrdenResponse pagar(UUID usuarioId, UUID ordenId, PagarRequest req, String requestId) {
		Orden orden = ordenRepo.findById(ordenId)
				.filter(o -> o.getUsuarioId().equals(usuarioId))
				.orElseThrow(() -> new NoEncontradoException("No existe la orden " + ordenId));

		// 1. Validar. El mismo error que una orden ajena (404) para no revelar que existe.
		switch (orden.getEstado()) {
			case PAGADA, EMITIDA -> {
				return OrdenResponse.de(orden);
			}
			case RESERVADA -> {
				if (!Instant.now().isBefore(orden.getVenceEn())) {
					throw new ReglaNegocioException("ORDEN_VENCIDA", "La reserva de esta orden vencio el " + orden.getVenceEn());
				}
			}
			case PENDIENTE_PAGO -> {
				// Un reintento: el primer intento no llego a saberse como termino.
			}
			default -> throw new ReglaNegocioException("ORDEN_NO_PAGABLE",
					"La orden esta %s y no se puede pagar".formatted(orden.getEstado()));
		}

		// 2. Proteger la reserva. Si vencio, ms-inventario responde 409 RESERVA_VENCIDA y aun no se toco nada.
		llamar("ms-inventario", () -> inventario.enPago(orden.getReservaId()));

		// 3. Marcar la orden. Si otra peticion ya la marco, da igual: ms-pagos decide quien cobra.
		if (orden.getEstado() == EstadoOrden.RESERVADA) {
			cambiarEstado(ordenId, EstadoOrden.RESERVADA, EstadoOrden.PENDIENTE_PAGO,
					"Pago iniciado: reserva protegida mientras se cobra");
		}

		// 4. Cobrar.
		PagoResponse pago;
		try {
			pago = llamar("ms-pagos", () -> pagos.cobrar(
					new CobrarRequest(ordenId, orden.getTotal(), req.numeroTarjeta(), req.titular())));
		} catch (ServicioNoDisponibleException e) {
			throw new PagoNoConfirmadoException("No se supo si ms-pagos cobro la orden " + ordenId + ": " + e.getMessage(), e);
		}

		return switch (pago.estado()) {
			case "APROBADO" -> aprobar(orden, pago, requestId);
			case "RECHAZADO" -> rechazar(orden, pago);
			default -> throw new PagoNoConfirmadoException(
					"ms-pagos respondio un estado inesperado para la orden %s: %s".formatted(ordenId, pago.estado()), null);
		};
	}

	/** APROBADO -> PAGADA y se avisa. Solo publica quien hizo el cambio de estado, no quien llega tarde. */
	private OrdenResponse aprobar(Orden orden, PagoResponse pago, String requestId) {
		boolean cambio = cambiarEstado(orden.getId(), EstadoOrden.PENDIENTE_PAGO, EstadoOrden.PAGADA,
				"Pago aprobado (referencia %s)".formatted(pago.referenciaPasarela()));

		Orden actual = ordenRepo.findById(orden.getId()).orElseThrow();
		if (cambio) {
			publicarOrdenPagada(actual, requestId);
		}
		return OrdenResponse.de(actual);
	}

	/**
	 * RECHAZADO -> la reserva vuelve a ACTIVA y la orden a RESERVADA, para reintentar con otra tarjeta.
	 *
	 * Si ms-inventario no responde, la orden se queda en PENDIENTE_PAGO junto con su reserva EN_PAGO:
	 * es un estado coherente, y el siguiente intento de pago lo retoma sin problema.
	 */
	private OrdenResponse rechazar(Orden orden, PagoResponse pago) {
		String motivo = pago.motivoRechazo() != null ? pago.motivoRechazo() : "la pasarela no dio motivo";
		try {
			llamar("ms-inventario", () -> inventario.activar(orden.getReservaId()));
			cambiarEstado(orden.getId(), EstadoOrden.PENDIENTE_PAGO, EstadoOrden.RESERVADA,
					"Pago rechazado (%s): la orden vuelve a RESERVADA".formatted(motivo));
		} catch (ServicioNoDisponibleException e) {
			log.warn("Orden {}: pago rechazado, pero no se pudo reactivar la reserva; sigue PENDIENTE_PAGO: {}",
					orden.getId(), e.getMessage());
		}
		throw new PagoRechazadoException(motivo);
	}

	/**
	 * orden.pagada: ms-inventario confirma la reserva y (fase 10) ms-tickets emite las entradas.
	 *
	 * Se publica DESPUES de guardar PAGADA, para que quien reciba el mensaje encuentre la orden ya pagada.
	 * Si el broker no confirma, el pago ya esta hecho y la orden PAGADA: no se le dice al comprador que
	 * fallo, se deja el error en el log. Es el hueco que cierra el patron outbox (guardar el mensaje en la
	 * misma transaccion y publicarlo despues), que queda fuera del alcance de las fases del plan.
	 */
	private void publicarOrdenPagada(Orden orden, String requestId) {
		try {
			publicador.publicar(Mensajeria.ORDEN_PAGADA, SobreMensaje.de(Mensajeria.ORDEN_PAGADA, requestId,
					new OrdenPagada(orden.getId(), orden.getUsuarioId(), orden.getEventoId(), orden.getZonaId(),
							orden.getCantidad(), orden.getReservaId(), orden.getTotal())));
		} catch (PublicacionFallidaException e) {
			log.error("Orden {} PAGADA pero orden.pagada NO se publico: la reserva no se confirmara ni se "
					+ "emitiran entradas hasta republicarla a mano: {}", orden.getId(), e.getMessage());
		}
	}

	/** Cambia el estado de la orden y deja la fila de auditoria, juntos. Devuelve false si otra peticion se adelanto. */
	private boolean cambiarEstado(UUID ordenId, EstadoOrden desde, EstadoOrden hacia, String motivo) {
		return Boolean.TRUE.equals(tx.execute(s -> {
			if (ordenRepo.cambiarEstado(ordenId, desde, hacia) == 0) {
				return false;
			}
			historialRepo.save(OrdenHistorial.registrar(ordenId, desde, hacia, motivo));
			return true;
		}));
	}

	@Transactional(readOnly = true)
	public OrdenResponse consultar(UUID ordenId, UUID usuarioId) {
		return ordenRepo.findById(ordenId)
				.filter(o -> o.getUsuarioId().equals(usuarioId))
				.map(OrdenResponse::de)
				.orElseThrow(() -> new NoEncontradoException("No existe la orden " + ordenId));
	}

	@Transactional(readOnly = true)
	public List<OrdenResponse> mias(UUID usuarioId) {
		return ordenRepo.findByUsuarioIdOrderByCreadaEnDesc(usuarioId).stream().map(OrdenResponse::de).toList();
	}

	/** Para otros servicios: la orden de cualquiera, con su historial completo. */
	@Transactional(readOnly = true)
	public OrdenInternaResponse interna(UUID ordenId) {
		Orden orden = ordenRepo.findById(ordenId)
				.orElseThrow(() -> new NoEncontradoException("No existe la orden " + ordenId));
		return OrdenInternaResponse.de(orden, historialRepo.findByOrdenIdOrderByFechaAscIdAsc(ordenId));
	}

	// -----------------------------------------------------------------

	/** Sin cabecera -> 400. Si no es un UUID no puede ser un pase real: es un pase invalido. */
	private UUID leerPase(String cabecera) {
		if (cabecera == null || cabecera.isBlank()) {
			throw new PaseRequeridoException("Falta la cabecera X-Pase-Compra");
		}
		try {
			return UUID.fromString(cabecera.trim());
		} catch (IllegalArgumentException e) {
			throw new PaseNoValidoException("El pase de compra no tiene un formato valido");
		}
	}

	/**
	 * Ejecuta una llamada a otro servicio. Las excepciones de negocio que produce
	 * ClienteErrorDecoder pasan tal cual; lo que lance Feign por si mismo (timeout, conexion
	 * rechazada, respuesta ilegible) se convierte en "servicio no disponible".
	 */
	private <T> T llamar(String servicio, Supplier<T> llamada) {
		try {
			return llamada.get();
		} catch (FeignException e) {
			throw new ServicioNoDisponibleException(servicio, e.getMessage(), e);
		}
	}

	/**
	 * Compensacion: devuelve el pase consumido. Si tampoco esto funciona (por ejemplo,
	 * ms-cola esta caido), el pase queda gastado y se deja constancia en el log para que
	 * se revise a mano: es el unico caso que queda sin red de seguridad.
	 */
	private void devolverPase(UUID pase, UUID usuarioId) {
		try {
			cola.devolverPase(pase.toString(), new DevolverPaseRequest(usuarioId));
			log.info("Pase {} devuelto al comprador {}: la orden no se pudo completar", pase, usuarioId);
		} catch (RuntimeException e) {
			// Si el fallo fue al CONSUMIR (no se sabe si llego), puede que no haya nada que devolver.
			log.error("No se pudo devolver el pase {} del comprador {}: si ya estaba USADO, revisar a mano: {}",
					pase, usuarioId, e.toString());
		}
	}
}
