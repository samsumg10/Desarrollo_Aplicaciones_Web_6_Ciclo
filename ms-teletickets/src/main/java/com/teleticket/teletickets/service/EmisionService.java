package com.teleticket.teletickets.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Emite las entradas de una orden pagada (CU-07).
 *
 * <pre>
 *   1. condiciones   se piden a ms-eventos          solo LEE
 *   2. entradas      N filas firmadas               transaccion local corta
 *   3. tickets.emitidos se anuncia a ms-ordenes
 *   4. mensaje_procesado se anota el mensaje
 * </pre>
 *
 * No es @Transactional a proposito: el paso 1 es una llamada HTTP y no debe mantener abierta una conexion
 * de base de datos (igual que OrdenService en ms-ordenes). Cada paso que escribe tiene su propia transaccion.
 *
 * Es seguro de repetir, que es lo que permite reintentar sin miedo:
 *   - las entradas son idempotentes por UNIQUE(orden_id, numero): repetir no crea otras;
 *   - el mensaje se anota DESPUES de anunciarlo. Si el servicio se cae entre el paso 2 y el 3, la reentrega
 *     encuentra las entradas ya hechas, no crea mas, y completa el anuncio que faltaba.
 */
@Service
public class EmisionService {

	private static final Logger log = LoggerFactory.getLogger(EmisionService.class);

	private final TicketRepository ticketRepo;
	private final MensajeProcesadoRepository procesadosRepo;
	private final EventoClient eventos;
	private final FirmaService firma;
	private final PublicadorEventos publicador;
	private final TransactionTemplate tx;

	public EmisionService(TicketRepository ticketRepo, MensajeProcesadoRepository procesadosRepo,
			EventoClient eventos, FirmaService firma, PublicadorEventos publicador, TransactionTemplate tx) {
		this.ticketRepo = ticketRepo;
		this.procesadosRepo = procesadosRepo;
		this.eventos = eventos;
		this.firma = firma;
		this.publicador = publicador;
		this.tx = tx;
	}

	/**
	 * Emite y anuncia las entradas de la orden del mensaje.
	 *
	 * @return true si este mensaje emitio y anuncio las entradas, false si era un mensaje repetido
	 * @throws MensajeInvalidoException si el mensaje no trae lo necesario (reintentarlo no lo arregla)
	 * @throws RuntimeException cualquier otro fallo (ms-eventos caido, base de datos, broker): es pasajero y
	 *         el listener reintenta
	 */
	public boolean emitir(SobreMensaje<OrdenPagada> sobre) {
		validar(sobre);

		if (procesadosRepo.existsById(sobre.eventoId())) {
			log.info("Mensaje {} repetido: sus entradas ya se emitieron y anunciaron, se descarta", sobre.eventoId());
			return false;
		}

		OrdenPagada orden = sobre.datos();

		// 1. PRB-06: las condiciones se copian de ms-eventos a cada entrada.
		CondicionesResponse condiciones = eventos.condiciones(orden.eventoId(), sobre.correlationId());
		if (condiciones == null || condiciones.condicionesIngreso() == null || condiciones.condicionesIngreso().isBlank()) {
			throw new IllegalStateException("ms-eventos no devolvio condiciones de ingreso para el evento " + orden.eventoId());
		}

		// 2. Las entradas.
		List<UUID> ticketIds = tx.execute(s -> crearEntradas(orden, condiciones.condicionesIngreso()));

		// 3. Avisar. Si el broker no confirma lanza PublicacionFallidaException y el listener reintenta.
		publicador.publicar(Mensajeria.TICKETS_EMITIDOS, SobreMensaje.de(Mensajeria.TICKETS_EMITIDOS,
				sobre.correlationId(),
				new TicketsEmitidos(orden.ordenId(), orden.usuarioId(), orden.eventoId(), ticketIds)));

		// 4. Anotar el mensaje.
		tx.executeWithoutResult(s -> procesadosRepo.registrar(sobre.eventoId()));
		return true;
	}

	/** Las entradas 1..N de la orden. Las que ya existan se dejan como estan. Devuelve los ids de todas. */
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

	/** Un mensaje sin orden, evento o cantidad no se arregla reintentando: se rechaza. */
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
