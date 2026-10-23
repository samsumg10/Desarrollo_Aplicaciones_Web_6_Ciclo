package com.teleticket.cola.service;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.teleticket.cola.dto.TurnoResponse;
import com.teleticket.cola.entity.Turno;
import com.teleticket.cola.exception.NoEncontradoException;
import com.teleticket.cola.repository.TurnoRepository;
import com.teleticket.cola.repository.VentaEventoRepository;

@Service
public class IngresoService {

	public record Ingreso(TurnoResponse turno, boolean creado) {
	}

	private final TurnoRepository turnoRepo;
	private final VentaEventoRepository ventaRepo;
	private final TurnoService turnoService;
	private final TransactionTemplate tx;

	public IngresoService(TurnoRepository turnoRepo, VentaEventoRepository ventaRepo,
			TurnoService turnoService, TransactionTemplate tx) {
		this.turnoRepo = turnoRepo;
		this.ventaRepo = ventaRepo;
		this.turnoService = turnoService;
		this.tx = tx;
	}

	public Ingreso ingresar(UUID eventoId, UUID usuarioId) {
		try {
			return tx.execute(s -> ingresarEnTransaccion(eventoId, usuarioId));
		} catch (DataIntegrityViolationException e) {
			return tx.execute(s -> turnoRepo.findByEventoIdAndUsuarioId(eventoId, usuarioId)
					.map(t -> new Ingreso(turnoService.vista(t), false))
					.orElseThrow(() -> e));
		}
	}

	private Ingreso ingresarEnTransaccion(UUID eventoId, UUID usuarioId) {
		var existente = turnoRepo.findByEventoIdAndUsuarioId(eventoId, usuarioId);
		if (existente.isPresent()) {
			return new Ingreso(turnoService.vista(existente.get()), false);
		}

		if (!ventaRepo.existsById(eventoId)) {
			throw new NoEncontradoException(
					"No hay sala de espera para el evento " + eventoId + ": no existe o aun no se publico");
		}

		Integer posicion = null;
		if (ventaRepo.tomarPosicionTardia(eventoId) == 1) {
			posicion = ventaRepo.ultimaPosicion(eventoId);
		}

		Turno turno = turnoRepo.saveAndFlush(Turno.crear(eventoId, usuarioId, posicion));
		return new Ingreso(turnoService.vista(turno), true);
	}
}
