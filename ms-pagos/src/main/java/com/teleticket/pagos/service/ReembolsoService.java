package com.teleticket.pagos.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.teleticket.mensajeria.Mensajeria;
import com.teleticket.mensajeria.PublicadorEventos;
import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.PagoReembolsado;
import com.teleticket.mensajeria.mensajes.ReembolsoSolicitado;
import com.teleticket.pagos.entity.EstadoPago;
import com.teleticket.pagos.entity.Pago;
import com.teleticket.pagos.exception.MensajeInvalidoException;
import com.teleticket.pagos.repository.PagoRepository;

/**
 * Devuelve el dinero de una orden (fase 11, RN-08): el paso de la saga que compensa al cobro de la fase 9.
 *
 * <pre>
 *   1. reembolso   INSERT en 'reembolso' + pago APROBADO -> REEMBOLSADO       una transaccion corta
 *   2. pago.reembolsado   se anuncia a ms-ordenes
 * </pre>
 *
 * Es seguro de repetir: UNIQUE(pago_id) impide devolver dos veces el mismo pago, y el anuncio se hace SIEMPRE,
 * incluso si el reembolso ya existia. Asi, si el servicio se cae entre 1 y 2, la reentrega no devuelve el dinero
 * otra vez pero si completa el anuncio que faltaba (la misma idea que ms-tickets en la fase 10).
 *
 * Por que el monto sale del pago y no del mensaje: lo que se devuelve es exactamente lo que se cobro.
 *
 * La pasarela es simulada y no tiene operacion de devolucion: el reembolso queda registrado en la base de datos.
 */
@Service
public class ReembolsoService {

	private static final Logger log = LoggerFactory.getLogger(ReembolsoService.class);

	private static final int MAX_MOTIVO = 200;

	private final PagoRepository pagoRepo;
	private final PublicadorEventos publicador;
	private final TransactionTemplate tx;

	public ReembolsoService(PagoRepository pagoRepo, PublicadorEventos publicador, TransactionTemplate tx) {
		this.pagoRepo = pagoRepo;
		this.publicador = publicador;
		this.tx = tx;
	}

	/**
	 * @return true si este mensaje devolvio el dinero, false si ya estaba devuelto (el anuncio se repite igual)
	 * @throws MensajeInvalidoException si el mensaje no trae la orden o la orden no tiene un pago cobrado
	 * @throws com.teleticket.mensajeria.PublicacionFallidaException si el broker no confirma (el listener reintenta)
	 */
	public boolean reembolsar(SobreMensaje<ReembolsoSolicitado> sobre) {
		if (sobre == null || sobre.datos() == null || sobre.datos().ordenId() == null) {
			throw new MensajeInvalidoException("El mensaje no trae la orden");
		}
		ReembolsoSolicitado pedido = sobre.datos();
		UUID ordenId = pedido.ordenId();

		Pago pago = pagoRepo.findByIdempotencyKey(ordenId.toString()).orElseThrow(() -> new MensajeInvalidoException(
				"reembolso.solicitado de la orden %s, que no tiene ningun pago".formatted(ordenId)));

		if (pago.getEstado() != EstadoPago.APROBADO && pago.getEstado() != EstadoPago.REEMBOLSADO) {
			throw new MensajeInvalidoException("La orden %s tiene un pago %s: no hay dinero cobrado que devolver"
					.formatted(ordenId, pago.getEstado()));
		}
		if (pedido.monto() != null && pedido.monto().compareTo(pago.getMonto()) != 0) {
			log.warn("Orden {}: se pidio reembolsar S/ {} pero se cobraron S/ {}. Se devuelve lo cobrado", ordenId,
					pedido.monto(), pago.getMonto());
		}

		String motivo = pedido.motivo() != null ? pedido.motivo() : "Reembolso solicitado";
		String motivoGuardado = motivo.length() > MAX_MOTIVO ? motivo.substring(0, MAX_MOTIVO) : motivo;

		boolean devuelto = Boolean.TRUE.equals(tx.execute(s -> {
			int nuevo = pagoRepo.registrarReembolso(UUID.randomUUID(), pago.getId(), pago.getMonto(), motivoGuardado);
			pagoRepo.marcarReembolsado(pago.getId());
			return nuevo == 1;
		}));

		publicador.publicar(Mensajeria.PAGO_REEMBOLSADO, SobreMensaje.de(Mensajeria.PAGO_REEMBOLSADO,
				sobre.correlationId(), new PagoReembolsado(ordenId, pago.getMonto(), motivo)));

		if (devuelto) {
			log.info("Orden {}: reembolso de S/ {} registrado ({}), se publico pago.reembolsado", ordenId,
					pago.getMonto(), motivo);
		} else {
			log.info("Orden {}: el reembolso ya estaba registrado, se repite pago.reembolsado por si el anuncio "
					+ "anterior se perdio", ordenId);
		}
		return devuelto;
	}
}
