package com.teleticket.inventario.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teleticket.inventario.exception.MensajeInvalidoException;
import com.teleticket.inventario.repository.MensajeProcesadoRepository;
import com.teleticket.inventario.repository.StockZonaRepository;
import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.EventoPublicado;
import com.teleticket.mensajeria.mensajes.ZonaPublicada;

/**
 * Crea el stock de un evento a partir del mensaje evento.publicado.
 *
 * Es la logica del consumidor, separada del listener para que el listener se
 * ocupe solo del ack/nack de RabbitMQ y esta clase solo del negocio.
 */
@Service
public class StockService {

	private static final Logger log = LoggerFactory.getLogger(StockService.class);

	private final StockZonaRepository stockRepo;
	private final MensajeProcesadoRepository procesadosRepo;

	public StockService(StockZonaRepository stockRepo, MensajeProcesadoRepository procesadosRepo) {
		this.stockRepo = stockRepo;
		this.procesadosRepo = procesadosRepo;
	}

	/**
	 * Crea una fila de stock por cada zona, con disponible = aforo.
	 *
	 * Idempotente: si el mensaje ya se proceso, no hace nada y devuelve false.
	 *
	 * Es una sola transaccion: el registro en mensaje_procesado y las filas de
	 * stock se guardan juntos o no se guarda nada. Asi, si algo falla a medias,
	 * el mensaje NO queda marcado como procesado y se podra reintentar completo.
	 * (Si la marca se guardara aparte, un fallo dejaria un mensaje "procesado"
	 * sin stock, y nadie volveria a crearlo.)
	 *
	 * @return true si se creo el stock, false si era un mensaje repetido
	 * @throws MensajeInvalidoException si el mensaje no trae lo necesario
	 */
	@Transactional
	public boolean crearStockDeEvento(SobreMensaje<EventoPublicado> sobre) {
		validar(sobre);

		if (procesadosRepo.registrar(sobre.eventoId()) == 0) {
			log.info("Mensaje {} repetido: ya se habia procesado, se descarta", sobre.eventoId());
			return false;
		}

		EventoPublicado evento = sobre.datos();
		for (ZonaPublicada zona : evento.zonas()) {
			// Si la zona ya tenia stock (mismo evento con otro mensaje), no se toca.
			int creadas = stockRepo.crearSiNoExiste(zona.id(), evento.id(), zona.aforo());
			log.info("Zona {} ({}): {}", zona.nombre(), zona.id(),
					creadas == 1 ? "stock creado con " + zona.aforo() : "ya tenia stock, sin cambios");
		}
		return true;
	}

	/** Un mensaje sin zonas o sin id no se arregla reintentando: se rechaza. */
	private void validar(SobreMensaje<EventoPublicado> sobre) {
		if (sobre == null || sobre.eventoId() == null) {
			throw new MensajeInvalidoException("El mensaje no trae eventoId");
		}
		EventoPublicado evento = sobre.datos();
		if (evento == null || evento.id() == null) {
			throw new MensajeInvalidoException("El mensaje no trae los datos del evento");
		}
		if (evento.zonas() == null || evento.zonas().isEmpty()) {
			throw new MensajeInvalidoException("El evento " + evento.id() + " no trae zonas");
		}
		for (ZonaPublicada z : evento.zonas()) {
			if (z.id() == null || z.aforo() == null || z.aforo() <= 0) {
				throw new MensajeInvalidoException("Zona invalida en el evento " + evento.id() + ": " + z);
			}
		}
	}
}
