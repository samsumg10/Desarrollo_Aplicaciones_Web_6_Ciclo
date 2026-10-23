package com.teleticket.cola.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.teleticket.cola.entity.EstadoTurno;
import com.teleticket.cola.entity.VentaEvento;
import com.teleticket.cola.repository.TurnoRepository;
import com.teleticket.cola.repository.VentaEventoRepository;

@Service
public class SorteoService {

	private static final Logger log = LoggerFactory.getLogger(SorteoService.class);

	private final VentaEventoRepository ventaRepo;
	private final TurnoRepository turnoRepo;
	private final TransactionTemplate tx;

	public SorteoService(VentaEventoRepository ventaRepo, TurnoRepository turnoRepo, TransactionTemplate tx) {
		this.ventaRepo = ventaRepo;
		this.turnoRepo = turnoRepo;
		this.tx = tx;
	}

	public void ejecutarPendientes() {
		for (UUID eventoId : ventaRepo.pendientesDeSorteo()) {
			try {
				sortear(eventoId);
			} catch (RuntimeException e) {
				log.error("Fallo el sorteo del evento {}; se reintenta en el proximo ciclo", eventoId, e);
			}
		}
		for (UUID eventoId : ventaRepo.conRezagados()) {
			try {
				ubicarRezagados(eventoId);
			} catch (RuntimeException e) {
				log.error("No se pudo ubicar a los rezagados del evento {}", eventoId, e);
			}
		}
	}

	void sortear(UUID eventoId) {
		tx.executeWithoutResult(s -> {
			if (ventaRepo.reclamarSorteo(eventoId) == 0) {
				return;
			}
			int sorteados = turnoRepo.sortear(eventoId);
			ventaRepo.fijarUltimaPosicion(eventoId);
			log.info("SORTEO del evento {}: {} personas ordenadas al azar", eventoId, sorteados);
		});
	}

	void ubicarRezagados(UUID eventoId) {
		tx.executeWithoutResult(s -> {
			VentaEvento venta = ventaRepo.bloquear(eventoId).orElseThrow();
			var rezagados = turnoRepo.findByEventoIdAndEstadoOrderByIngresoEnAscIdAsc(eventoId, EstadoTurno.EN_SALA);
			rezagados.forEach(t -> t.ubicarEnCola(venta.siguientePosicion()));
			if (!rezagados.isEmpty()) {
				log.info("Evento {}: {} persona(s) que entraron durante el sorteo se ubican al final",
						eventoId, rezagados.size());
			}
		});
	}
}
