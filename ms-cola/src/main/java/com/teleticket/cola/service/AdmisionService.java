package com.teleticket.cola.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.teleticket.cola.config.ColaProperties;
import com.teleticket.cola.entity.PaseCompra;
import com.teleticket.cola.entity.Turno;
import com.teleticket.cola.entity.VentaEvento;
import com.teleticket.cola.repository.PaseCompraRepository;
import com.teleticket.cola.repository.TurnoRepository;
import com.teleticket.cola.repository.VentaEventoRepository;
import com.teleticket.mensajeria.Mensajeria;
import com.teleticket.mensajeria.PublicacionFallidaException;
import com.teleticket.mensajeria.PublicadorEventos;
import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.TurnoHabilitado;

@Service
public class AdmisionService {

	private static final Logger log = LoggerFactory.getLogger(AdmisionService.class);

	private final TurnoRepository turnoRepo;
	private final VentaEventoRepository ventaRepo;
	private final PaseCompraRepository paseRepo;
	private final PublicadorEventos publicador;
	private final ColaProperties props;
	private final TransactionTemplate tx;

	public AdmisionService(TurnoRepository turnoRepo, VentaEventoRepository ventaRepo,
			PaseCompraRepository paseRepo, PublicadorEventos publicador, ColaProperties props,
			TransactionTemplate tx) {
		this.turnoRepo = turnoRepo;
		this.ventaRepo = ventaRepo;
		this.paseRepo = paseRepo;
		this.publicador = publicador;
		this.props = props;
		this.tx = tx;
	}

	public void ejecutarCiclo() {
		tx.executeWithoutResult(s -> {
			int pases = paseRepo.vencerPasados();
			int turnos = turnoRepo.expirarConPaseVencido();
			if (pases > 0 || turnos > 0) {
				log.info("Vencieron {} pase(s) sin usar; {} turno(s) pasan a EXPIRADO", pases, turnos);
			}
		});

		List<TurnoHabilitado> avisos = new ArrayList<>();
		for (UUID eventoId : turnoRepo.eventosConTurnosEnCola()) {
			try {
				avisos.addAll(tx.execute(s -> habilitarSiguientes(eventoId)));
			} catch (RuntimeException e) {
				log.error("Fallo la admision del evento {}; se reintenta en el proximo ciclo", eventoId, e);
			}
		}
		publicarAvisos(avisos);
	}

	private List<TurnoHabilitado> habilitarSiguientes(UUID eventoId) {
		VentaEvento venta = ventaRepo.findById(eventoId).orElseThrow();
		List<Turno> turnos = turnoRepo.siguientesEnCola(eventoId, venta.getCapacidadPorCiclo());

		List<TurnoHabilitado> avisos = new ArrayList<>();
		for (Turno turno : turnos) {
			turno.habilitar();
			PaseCompra pase = PaseCompra.crear(turno, Duration.ofMinutes(props.paseMinutos()));
			paseRepo.save(pase);
			avisos.add(new TurnoHabilitado(turno.getId(), turno.getUsuarioId(), eventoId,
					pase.getCodigo().toString(), pase.getExpiraEn()));
		}
		if (!turnos.isEmpty()) {
			log.info("ADMISION evento {}: {} turno(s) habilitados (posiciones {} a {})", eventoId, turnos.size(),
					turnos.get(0).getPosicion(), turnos.get(turnos.size() - 1).getPosicion());
		}
		return avisos;
	}

	private void publicarAvisos(List<TurnoHabilitado> avisos) {
		for (TurnoHabilitado aviso : avisos) {
			try {
				publicador.publicar(Mensajeria.TURNO_HABILITADO,
						SobreMensaje.de(Mensajeria.TURNO_HABILITADO, null, aviso));
			} catch (PublicacionFallidaException e) {
				log.warn("No se pudo publicar turno.habilitado; se omiten los avisos de este ciclo: {}",
						e.getMessage());
				return;
			}
		}
	}
}
