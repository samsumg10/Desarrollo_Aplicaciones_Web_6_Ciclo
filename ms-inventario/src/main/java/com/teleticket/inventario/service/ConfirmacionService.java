package com.teleticket.inventario.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teleticket.inventario.entity.Reserva;
import com.teleticket.inventario.exception.MensajeInvalidoException;
import com.teleticket.inventario.repository.MensajeProcesadoRepository;
import com.teleticket.inventario.repository.ReservaRepository;
import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.OrdenPagada;

/**
 * Confirma la reserva de una orden a partir del mensaje orden.pagada.
 *
 * Es la logica del consumidor, separada del listener (igual que StockService con evento.publicado).
 * Confirmar es lo que hace que las entradas ya no puedan volver al stock.
 */
@Service
public class ConfirmacionService {

	private static final Logger log = LoggerFactory.getLogger(ConfirmacionService.class);

	private final ReservaRepository reservaRepo;
	private final MensajeProcesadoRepository procesadosRepo;

	public ConfirmacionService(ReservaRepository reservaRepo, MensajeProcesadoRepository procesadosRepo) {
		this.reservaRepo = reservaRepo;
		this.procesadosRepo = procesadosRepo;
	}

	/**
	 * Pasa la reserva a CONFIRMADA.
	 *
	 * Idempotente de dos maneras: mensaje_procesado descarta el mismo mensaje repetido, y confirmar es un
	 * UPDATE condicional, asi que un orden.pagada distinto para la misma orden (por ejemplo, una
	 * republicacion manual) tampoco cambia nada la segunda vez.
	 *
	 * Una sola transaccion: la marca de mensaje_procesado y la confirmacion se guardan juntas o ninguna.
	 *
	 * @return true si la reserva quedo confirmada por este mensaje, false si el mensaje era repetido o
	 *         la reserva ya estaba confirmada
	 * @throws MensajeInvalidoException si el mensaje no trae lo necesario o la reserva no existe
	 */
	@Transactional
	public boolean confirmarReserva(SobreMensaje<OrdenPagada> sobre) {
		validar(sobre);

		if (procesadosRepo.registrar(sobre.eventoId()) == 0) {
			log.info("Mensaje {} repetido: ya se habia procesado, se descarta", sobre.eventoId());
			return false;
		}

		OrdenPagada pago = sobre.datos();
		Reserva reserva = reservaRepo.findById(pago.reservaId()).orElseThrow(() -> new MensajeInvalidoException(
				"orden.pagada de la orden %s apunta a la reserva %s, que no existe"
						.formatted(pago.ordenId(), pago.reservaId())));

		if (reservaRepo.confirmar(reserva.getId()) == 1) {
			log.info("Reserva {} de la orden {} CONFIRMADA ({} entradas)", reserva.getId(), pago.ordenId(),
					reserva.getCantidad());
			return true;
		}

		// 0 filas: ya estaba CONFIRMADA (nada que hacer) o LIBERADA (las entradas volvieron al stock).
		switch (reserva.getEstado()) {
			case LIBERADA -> log.error("Orden {} PAGADA pero su reserva {} ya estaba LIBERADA: las entradas pudieron "
					+ "venderse a otra persona. Hay que reembolsar la orden o reponer el stock", pago.ordenId(),
					reserva.getId());
			default -> log.info("La reserva {} de la orden {} ya estaba {}: sin cambios", reserva.getId(),
					pago.ordenId(), reserva.getEstado());
		}
		return false;
	}

	/** Un mensaje sin orden o sin reserva no se arregla reintentando: se rechaza. */
	private void validar(SobreMensaje<OrdenPagada> sobre) {
		if (sobre == null || sobre.eventoId() == null) {
			throw new MensajeInvalidoException("El mensaje no trae eventoId");
		}
		OrdenPagada pago = sobre.datos();
		if (pago == null || pago.ordenId() == null || pago.reservaId() == null) {
			throw new MensajeInvalidoException("El mensaje no trae la orden y la reserva");
		}
	}
}
