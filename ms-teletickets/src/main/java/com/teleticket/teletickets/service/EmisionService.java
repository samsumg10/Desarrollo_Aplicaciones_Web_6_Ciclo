package com.teleticket.teletickets.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.teleticket.mensajeria.Mensajeria;
import com.teleticket.mensajeria.PublicadorEventos;
import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.OrdenPagada;
import com.teleticket.mensajeria.mensajes.TicketsEmitidos;
import com.teleticket.teletickets.client.EventoClient;
import com.teleticket.teletickets.client.dto.CondicionesResponse;
import com.teleticket.teletickets.entity.Ticket;
import com.teleticket.teletickets.exception.MensajeInvalidoException;
import com.teleticket.teletickets.repository.MensajeProcesadoRepository;
import com.teleticket.teletickets.repository.TicketRepository;

@Service
public class EmisionService {

	private static final Logger log = LoggerFactory.getLogger(EmisionService.class);

	private final TicketRepository ticketRepo;
	private final MensajeProcesadoRepository procesadosRepo;
	private final EventoClient eventos;
	private final FirmaService firma;
	private final PublicadorEventos publicador;
	private final TransactionTemplate tx;
	private final boolean simularFallo;

	public EmisionService(TicketRepository ticketRepo, MensajeProcesadoRepository procesadosRepo,
			EventoClient eventos, FirmaService firma, PublicadorEventos publicador, TransactionTemplate tx,
			@Value("${teleticket.tickets.simular-fallo-emision:false}") boolean simularFallo) {
		this.ticketRepo = ticketRepo;
		this.procesadosRepo = procesadosRepo;
		this.eventos = eventos;
		this.firma = firma;
		this.publicador = publicador;
		this.tx = tx;
		this.simularFallo = simularFallo;
	}

	public boolean emitir(SobreMensaje<OrdenPagada> sobre) {
		validar(sobre);

		if (procesadosRepo.existsById(sobre.eventoId())) {
			log.info("Mensaje {} repetido: sus entradas ya se emitieron y anunciaron, se descarta", sobre.eventoId());
			return false;
		}

		OrdenPagada orden = sobre.datos();

		CondicionesResponse condiciones = eventos.condiciones(orden.eventoId(), sobre.correlationId());
		if (condiciones == null || condiciones.condicionesIngreso() == null || condiciones.condicionesIngreso().isBlank()) {
			throw new IllegalStateException("ms-eventos no devolvio condiciones de ingreso para el evento " + orden.eventoId());
		}

		List<UUID> ticketIds = tx.execute(s -> crearEntradas(orden, condiciones.condicionesIngreso()));

		if (simularFallo) {
			throw new IllegalStateException("Fallo simulado (teleticket.tickets.simular-fallo-emision=true)");
		}

		publicador.publicar(Mensajeria.TICKETS_EMITIDOS, SobreMensaje.de(Mensajeria.TICKETS_EMITIDOS,
				sobre.correlationId(),
				new TicketsEmitidos(orden.ordenId(), orden.usuarioId(), orden.eventoId(), ticketIds)));

		tx.executeWithoutResult(s -> procesadosRepo.registrar(sobre.eventoId()));
		return true;
	}

	private List<UUID> crearEntradas(OrdenPagada orden, String condiciones) {
		int nuevas = 0;
		for (int numero = 1; numero <= orden.cantidad(); numero++) {
			UUID codigo = UUID.randomUUID();
			nuevas += ticketRepo.emitir(UUID.randomUUID(), orden.ordenId(), numero, orden.usuarioId(),
					orden.eventoId(), orden.zonaId(), codigo, firma.firmar(codigo, orden.eventoId()), condiciones);
		}

		List<UUID> ids = ticketRepo.findByOrdenIdOrderByNumero(orden.ordenId()).stream().map(Ticket::getId).toList();
		if (nuevas == 0) {
			log.info("Orden {}: ya tenia sus {} entradas, no se emite ninguna otra", orden.ordenId(), ids.size());
		} else {
			log.info("Orden {}: {} entradas emitidas con firma HMAC", orden.ordenId(), nuevas);
		}
		return ids;
	}

	private void validar(SobreMensaje<OrdenPagada> sobre) {
		if (sobre == null || sobre.eventoId() == null) {
			throw new MensajeInvalidoException("El mensaje no trae eventoId");
		}
		OrdenPagada orden = sobre.datos();
		if (orden == null || orden.ordenId() == null || orden.usuarioId() == null || orden.eventoId() == null
				|| orden.zonaId() == null) {
			throw new MensajeInvalidoException("El mensaje no trae la orden, el comprador, el evento y la zona");
		}
		if (orden.cantidad() == null || orden.cantidad() < 1) {
			throw new MensajeInvalidoException("La orden %s no trae una cantidad valida".formatted(orden.ordenId()));
		}
	}
}
