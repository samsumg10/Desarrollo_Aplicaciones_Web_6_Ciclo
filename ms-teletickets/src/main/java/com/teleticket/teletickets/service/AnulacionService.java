package com.teleticket.teletickets.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.OrdenReembolsada;
import com.teleticket.teletickets.exception.MensajeInvalidoException;
import com.teleticket.teletickets.repository.TicketRepository;

/**
 * Anula las entradas de una orden reembolsada (fase 11): el ultimo paso de la saga en este servicio.
 *
 * Si la emision fallo antes de guardar ninguna entrada, no hay nada que anular y el mensaje es un no-op.
 * Si fallo despues (las entradas se guardaron pero no se anunciaron), el comprador ya recibio su dinero y
 * esas entradas no deben abrir ninguna puerta: pasan a ANULADO.
 *
 * Idempotente sin tabla de mensajes: solo se anulan las EMITIDO, asi que repetir no cambia nada.
 */
@Service
public class AnulacionService {

	private static final Logger log = LoggerFactory.getLogger(AnulacionService.class);

	private final TicketRepository ticketRepo;

	public AnulacionService(TicketRepository ticketRepo) {
		this.ticketRepo = ticketRepo;
	}

	/**
	 * @return cuantas entradas se anularon
	 * @throws MensajeInvalidoException si el mensaje no trae la orden
	 */
	@Transactional
	public int anular(SobreMensaje<OrdenReembolsada> sobre) {
		if (sobre == null || sobre.datos() == null || sobre.datos().ordenId() == null) {
			throw new MensajeInvalidoException("El mensaje no trae la orden");
		}
		var ordenId = sobre.datos().ordenId();
		int anuladas = ticketRepo.anularDeOrden(ordenId);
		if (anuladas == 0) {
			log.info("Orden {} reembolsada: no hay entradas emitidas que anular", ordenId);
		} else {
			log.info("Orden {} reembolsada: {} entradas ANULADAS", ordenId, anuladas);
		}
		return anuladas;
	}
}
