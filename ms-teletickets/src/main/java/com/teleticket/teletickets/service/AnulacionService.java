package com.teleticket.teletickets.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.OrdenReembolsada;
import com.teleticket.teletickets.exception.MensajeInvalidoException;
import com.teleticket.teletickets.repository.TicketRepository;

@Service
public class AnulacionService {

	private static final Logger log = LoggerFactory.getLogger(AnulacionService.class);

	private final TicketRepository ticketRepo;

	public AnulacionService(TicketRepository ticketRepo) {
		this.ticketRepo = ticketRepo;
	}

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
