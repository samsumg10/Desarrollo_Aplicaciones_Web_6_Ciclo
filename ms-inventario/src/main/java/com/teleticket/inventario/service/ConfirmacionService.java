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

@Service
public class ConfirmacionService {

	private static final Logger log = LoggerFactory.getLogger(ConfirmacionService.class);

	private final ReservaRepository reservaRepo;
	private final MensajeProcesadoRepository procesadosRepo;

	public ConfirmacionService(ReservaRepository reservaRepo, MensajeProcesadoRepository procesadosRepo) {
		this.reservaRepo = reservaRepo;
		this.procesadosRepo = procesadosRepo;
	}

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

		switch (reserva.getEstado()) {
			case LIBERADA -> log.error("Orden {} PAGADA pero su reserva {} ya estaba LIBERADA: las entradas pudieron "
					+ "venderse a otra persona. Hay que reembolsar la orden o reponer el stock", pago.ordenId(),
					reserva.getId());
			default -> log.info("La reserva {} de la orden {} ya estaba {}: sin cambios", reserva.getId(),
					pago.ordenId(), reserva.getEstado());
		}
		return false;
	}

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
