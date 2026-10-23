package com.teleticket.cola.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teleticket.cola.config.ColaProperties;
import com.teleticket.cola.exception.MensajeInvalidoException;
import com.teleticket.cola.repository.MensajeProcesadoRepository;
import com.teleticket.cola.repository.VentaEventoRepository;
import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.EventoPublicado;

@Service
public class EventoVentaService {

	private static final Logger log = LoggerFactory.getLogger(EventoVentaService.class);

	private final VentaEventoRepository ventaRepo;
	private final MensajeProcesadoRepository procesadosRepo;
	private final ColaProperties props;

	public EventoVentaService(VentaEventoRepository ventaRepo, MensajeProcesadoRepository procesadosRepo,
			ColaProperties props) {
		this.ventaRepo = ventaRepo;
		this.procesadosRepo = procesadosRepo;
		this.props = props;
	}

	@Transactional
	public boolean registrarEvento(SobreMensaje<EventoPublicado> sobre) {
		validar(sobre);

		if (procesadosRepo.registrar(sobre.eventoId()) == 0) {
			log.info("Mensaje {} repetido: ya se habia procesado, se descarta", sobre.eventoId());
			return false;
		}

		EventoPublicado evento = sobre.datos();
		int creada = ventaRepo.crearSiNoExiste(evento.id(), evento.nombre(), evento.inicioVenta(),
				props.capacidadPorCiclo());
		log.info("Evento {} ({}): {}; sorteo a las {}", evento.nombre(), evento.id(),
				creada == 1 ? "sala de espera abierta" : "ya tenia sala, sin cambios", evento.inicioVenta());
		return true;
	}

	private void validar(SobreMensaje<EventoPublicado> sobre) {
		if (sobre == null || sobre.eventoId() == null) {
			throw new MensajeInvalidoException("El mensaje no trae eventoId");
		}
		EventoPublicado evento = sobre.datos();
		if (evento == null || evento.id() == null) {
			throw new MensajeInvalidoException("El mensaje no trae los datos del evento");
		}
		if (evento.inicioVenta() == null || evento.nombre() == null) {
			throw new MensajeInvalidoException("El evento " + evento.id() + " no trae nombre u hora de venta");
		}
	}
}
