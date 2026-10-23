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

/**
 * El centro de la saga de reembolso (fase 11, RN-08): ms-ordenes decide sobre la orden y orquesta a los demas.
 *
 * <pre>
 *   emision.fallida   ->  reembolso.solicitado     (orden PAGADA, todavia no cambia de estado)
 *   pago.reembolsado  ->  orden REEMBOLSADA  ->  orden.reembolsada
 * </pre>
 *
 * La orden pasa a REEMBOLSADA recien cuando ms-pagos confirma que devolvio el dinero. Mientras tanto sigue
 * PAGADA: asi nunca existe un estado "reembolso a medias" que alguien tenga que resolver a mano.
 *
 * Ninguno de los dos pasos necesita una tabla de mensajes procesados: ambos son seguros de repetir.
 * Pedir el reembolso dos veces no devuelve el dinero dos veces (ms-pagos tiene UNIQUE(pago_id)), y
 * marcar REEMBOLSADA es un UPDATE condicional.
 */
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

	/**
	 * emision.fallida: pide a ms-pagos que devuelva el dinero de la orden.
	 *
	 * Solo lo pide si la orden sigue PAGADA. Si ya esta EMITIDA las entradas existen (el aviso llego tarde, por
	 * ejemplo tras republicar a mano orden.pagada) y reembolsar dejaria al comprador sin lo que pago y con
	 * el dinero devuelto; si ya esta REEMBOLSADA no hay nada que pedir.
	 *
	 * @return true si se publico reembolso.solicitado
	 * @throws MensajeInvalidoException si el mensaje no trae la orden o la orden no existe
	 * @throws com.teleticket.mensajeria.PublicacionFallidaException si el broker no confirma (el listener reintenta)
	 */
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

	/**
	 * pago.reembolsado: la orden pasa a REEMBOLSADA, con su fila de historial, y se anuncia orden.reembolsada.
	 *
	 * El cambio y el anuncio no son atomicos (no hay outbox, fuera del plan). Si el servicio se cae entre uno y
	 * otro, el mensaje se reentrega, encuentra la orden ya REEMBOLSADA y completa el anuncio. Por eso un
	 * pago.reembolsado repetido vuelve a publicar orden.reembolsada: ms-inventario y ms-tickets son idempotentes.
	 *
	 * @return true si este mensaje cambio la orden, false si ya estaba REEMBOLSADA
	 * @throws MensajeInvalidoException si el mensaje no trae la orden, la orden no existe o no puede reembolsarse
	 */
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
				// Otra copia del mensaje (o un tickets.emitidos) se adelanto: se relee y se decide con el estado real.
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

	/** EMITIDA o PAGADA -> REEMBOLSADA con su auditoria, en una transaccion. false si el estado ya no era {@code desde}. */
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
