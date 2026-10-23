package com.teleticket.ordenes.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.teleticket.mensajeria.Mensajeria;
import com.teleticket.mensajeria.PublicadorEventos;
import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.EmisionFallida;
import com.teleticket.mensajeria.mensajes.OrdenReembolsada;
import com.teleticket.mensajeria.mensajes.PagoReembolsado;
import com.teleticket.mensajeria.mensajes.ReembolsoSolicitado;
import com.teleticket.ordenes.entity.EstadoOrden;
import com.teleticket.ordenes.entity.Orden;
import com.teleticket.ordenes.entity.OrdenHistorial;
import com.teleticket.ordenes.exception.MensajeInvalidoException;
import com.teleticket.ordenes.repository.OrdenHistorialRepository;
import com.teleticket.ordenes.repository.OrdenRepository;

@Service
public class ReembolsoService {

	private static final Logger log = LoggerFactory.getLogger(ReembolsoService.class);

	private final OrdenRepository ordenRepo;
	private final OrdenHistorialRepository historialRepo;
	private final PublicadorEventos publicador;
	private final TransactionTemplate tx;

	public ReembolsoService(OrdenRepository ordenRepo, OrdenHistorialRepository historialRepo,
			PublicadorEventos publicador, TransactionTemplate tx) {
		this.ordenRepo = ordenRepo;
		this.historialRepo = historialRepo;
		this.publicador = publicador;
		this.tx = tx;
	}

	public boolean solicitarReembolso(SobreMensaje<EmisionFallida> sobre) {
		if (sobre == null || sobre.datos() == null || sobre.datos().ordenId() == null) {
			throw new MensajeInvalidoException("El mensaje no trae la orden");
		}
		EmisionFallida fallo = sobre.datos();
		Orden orden = ordenRepo.findById(fallo.ordenId()).orElseThrow(() -> new MensajeInvalidoException(
				"emision.fallida apunta a la orden %s, que no existe".formatted(fallo.ordenId())));

		if (orden.getEstado() != EstadoOrden.PAGADA) {
			log.warn("emision.fallida de la orden {}, pero esta {} y no PAGADA: no se pide el reembolso",
					orden.getId(), orden.getEstado());
			return false;
		}

		String motivo = fallo.motivo() != null ? fallo.motivo() : "La emision de las entradas fallo";
		publicador.publicar(Mensajeria.REEMBOLSO_SOLICITADO, SobreMensaje.de(Mensajeria.REEMBOLSO_SOLICITADO,
				sobre.correlationId(), new ReembolsoSolicitado(orden.getId(), orden.getTotal(), motivo)));
		log.warn("Orden {} PAGADA sin entradas: se pidio el reembolso de S/ {} ({})", orden.getId(),
				orden.getTotal(), motivo);
		return true;
	}

	public boolean marcarReembolsada(SobreMensaje<PagoReembolsado> sobre) {
		if (sobre == null || sobre.datos() == null || sobre.datos().ordenId() == null) {
			throw new MensajeInvalidoException("El mensaje no trae la orden");
		}
		PagoReembolsado reembolso = sobre.datos();
		UUID ordenId = reembolso.ordenId();
		Orden orden = ordenRepo.findById(ordenId).orElseThrow(() -> new MensajeInvalidoException(
				"pago.reembolsado apunta a la orden %s, que no existe".formatted(ordenId)));

		if (reembolso.monto() != null && reembolso.monto().compareTo(orden.getTotal()) != 0) {
			log.warn("Orden {}: se reembolso S/ {} y la orden era de S/ {}", ordenId, reembolso.monto(),
					orden.getTotal());
		}

		boolean cambio = false;
		if (orden.getEstado() != EstadoOrden.REEMBOLSADA) {
			if (!orden.getEstado().puedePasarA(EstadoOrden.REEMBOLSADA)) {
				throw new MensajeInvalidoException("La orden %s esta %s y no puede pasar a REEMBOLSADA"
						.formatted(ordenId, orden.getEstado()));
			}
			String motivo = reembolso.motivo() != null ? reembolso.motivo() : "Pago reembolsado";
			cambio = reembolsar(ordenId, orden.getEstado(), motivo, reembolso.monto());
			if (!cambio) {
				EstadoOrden ahora = estadoActual(ordenId).orElseThrow();
				if (ahora != EstadoOrden.REEMBOLSADA) {
					cambio = reembolsar(ordenId, ahora, motivo, reembolso.monto());
				}
			}
		}

		Orden actual = ordenRepo.findById(ordenId).orElseThrow();
		publicador.publicar(Mensajeria.ORDEN_REEMBOLSADA, SobreMensaje.de(Mensajeria.ORDEN_REEMBOLSADA,
				sobre.correlationId(), new OrdenReembolsada(actual.getId(), actual.getUsuarioId(),
						actual.getEventoId(), actual.getZonaId(), actual.getCantidad(), actual.getReservaId())));
		if (cambio) {
			log.info("Orden {} REEMBOLSADA, se publico orden.reembolsada", ordenId);
		} else {
			log.info("La orden {} ya estaba REEMBOLSADA: se repite orden.reembolsada por si el anuncio anterior se perdio",
					ordenId);
		}
		return cambio;
	}

	private boolean reembolsar(UUID ordenId, EstadoOrden desde, String motivo, BigDecimal monto) {
		return Boolean.TRUE.equals(tx.execute(s -> {
			if (ordenRepo.cambiarEstado(ordenId, desde, EstadoOrden.REEMBOLSADA) == 0) {
				return false;
			}
			String detalle = monto != null ? "%s (S/ %s devueltos)".formatted(motivo, monto) : motivo;
			historialRepo.save(OrdenHistorial.registrar(ordenId, desde, EstadoOrden.REEMBOLSADA,
					detalle.length() > 300 ? detalle.substring(0, 300) : detalle));
			return true;
		}));
	}

	private Optional<EstadoOrden> estadoActual(UUID ordenId) {
		return ordenRepo.findById(ordenId).map(Orden::getEstado);
	}
}
