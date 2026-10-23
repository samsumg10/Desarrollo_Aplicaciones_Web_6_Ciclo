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

	public OrdenResponse crear(UUID usuarioId, String paseCabecera, CrearOrdenRequest req) {
		UUID pase = leerPase(paseCabecera);

		if (ordenRepo.existsByPaseCodigo(pase)) {
			throw new PaseNoValidoException("Este pase ya se uso para crear una orden");
		}

		ZonaInternaResponse zona = llamar("ms-eventos", () -> eventos.datosDeZona(req.eventoId(), req.zonaId()));
		if (!zona.ventaAbierta()) {
			throw new ReglaNegocioException("VENTA_CERRADA",
					"La venta de este evento aun no esta abierta o ya termino");
		}
		if (req.cantidad() > zona.maxPorCliente()) {
			throw new ReglaNegocioException("MAXIMO_EXCEDIDO",
					"Maximo %d entradas por comprador".formatted(zona.maxPorCliente()));
		}

		try {
			llamar("ms-cola", () -> cola.consumirPase(pase.toString(), new ConsumirPaseRequest(usuarioId, req.eventoId())));
		} catch (ServicioNoDisponibleException e) {
			devolverPase(pase, usuarioId);
			throw e;
		}

		try {
			UUID ordenId = UUID.nameUUIDFromBytes(("orden:" + pase).getBytes(StandardCharsets.UTF_8));
			ReservaResponse reserva = llamar("ms-inventario", () -> inventario
					.reservar(new CrearReservaRequest(ordenId, req.zonaId(), req.cantidad())));
			return guardar(ordenId, usuarioId, req, zona, pase, reserva);
		} catch (PaseNoValidoException e) {
			throw e;
		} catch (RuntimeException e) {
			devolverPase(pase, usuarioId);
			throw e;
		}
	}

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

	public OrdenResponse pagar(UUID usuarioId, UUID ordenId, PagarRequest req, String requestId) {
		Orden orden = ordenRepo.findById(ordenId)
				.filter(o -> o.getUsuarioId().equals(usuarioId))
				.orElseThrow(() -> new NoEncontradoException("No existe la orden " + ordenId));

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
			}
			default -> throw new ReglaNegocioException("ORDEN_NO_PAGABLE",
					"La orden esta %s y no se puede pagar".formatted(orden.getEstado()));
		}

		llamar("ms-inventario", () -> inventario.enPago(orden.getReservaId()));

		if (orden.getEstado() == EstadoOrden.RESERVADA) {
			cambiarEstado(ordenId, EstadoOrden.RESERVADA, EstadoOrden.PENDIENTE_PAGO,
					"Pago iniciado: reserva protegida mientras se cobra");
		}

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

	private OrdenResponse aprobar(Orden orden, PagoResponse pago, String requestId) {
		boolean cambio = cambiarEstado(orden.getId(), EstadoOrden.PENDIENTE_PAGO, EstadoOrden.PAGADA,
				"Pago aprobado (referencia %s)".formatted(pago.referenciaPasarela()));

		Orden actual = ordenRepo.findById(orden.getId()).orElseThrow();
		if (cambio) {
			publicarOrdenPagada(actual, requestId);
		}
		return OrdenResponse.de(actual);
	}

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

	@Transactional(readOnly = true)
	public OrdenInternaResponse interna(UUID ordenId) {
		Orden orden = ordenRepo.findById(ordenId)
				.orElseThrow(() -> new NoEncontradoException("No existe la orden " + ordenId));
		return OrdenInternaResponse.de(orden, historialRepo.findByOrdenIdOrderByFechaAscIdAsc(ordenId));
	}

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

	private <T> T llamar(String servicio, Supplier<T> llamada) {
		try {
			return llamada.get();
		} catch (FeignException e) {
			throw new ServicioNoDisponibleException(servicio, e.getMessage(), e);
		}
	}

	private void devolverPase(UUID pase, UUID usuarioId) {
		try {
			cola.devolverPase(pase.toString(), new DevolverPaseRequest(usuarioId));
			log.info("Pase {} devuelto al comprador {}: la orden no se pudo completar", pase, usuarioId);
		} catch (RuntimeException e) {
			log.error("No se pudo devolver el pase {} del comprador {}: si ya estaba USADO, revisar a mano: {}",
					pase, usuarioId, e.toString());
		}
	}
}
