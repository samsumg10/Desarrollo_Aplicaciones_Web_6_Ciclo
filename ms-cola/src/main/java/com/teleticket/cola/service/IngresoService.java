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

/**
 * Entrar a la sala de espera (CU-03).
 *
 * Es lo que recibe la avalancha, asi que hace lo minimo: un turno por persona,
 * sin llamar a ningun otro servicio.
 */
@Service
public class IngresoService {

	/** El turno y si se acaba de crear (para responder 201 o 200). */
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

	/**
	 * Crea el turno de la persona, o devuelve el que ya tenia (RN-04).
	 *
	 * Idempotente: volver a iniciar sesion, o entrar desde otro dispositivo,
	 * devuelve el mismo turno y no se pierde el lugar.
	 *
	 * Si dos peticiones de la misma persona llegan a la vez, ambas pasan la
	 * comprobacion previa y UNIQUE(evento_id, usuario_id) rechaza a la segunda.
	 * Esa transaccion se deshace entera (tambien el contador de posicion, si lo
	 * habia tomado) y aqui, ya fuera de ella, se devuelve el turno ganador.
	 * Por eso se usa TransactionTemplate y no @Transactional: hay que poder
	 * atrapar la excepcion DESPUES de que la transaccion termino.
	 */
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

		// RN-02: si el sorteo ya se hizo, se toma la ultima posicion + 1 y se nace EN_COLA.
		// Si aun no (el UPDATE afecta 0 filas), se nace EN_SALA, sin posicion.
		Integer posicion = null;
		if (ventaRepo.tomarPosicionTardia(eventoId) == 1) {
			posicion = ventaRepo.ultimaPosicion(eventoId);
		}

		Turno turno = turnoRepo.saveAndFlush(Turno.crear(eventoId, usuarioId, posicion));
		return new Ingreso(turnoService.vista(turno), true);
	}
}
