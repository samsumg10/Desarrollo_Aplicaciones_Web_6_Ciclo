package com.teleticket.cola.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teleticket.cola.config.ColaProperties;
import com.teleticket.cola.dto.PaseResponse;
import com.teleticket.cola.dto.TurnoResponse;
import com.teleticket.cola.entity.EstadoTurno;
import com.teleticket.cola.entity.Turno;
import com.teleticket.cola.entity.VentaEvento;
import com.teleticket.cola.exception.NoEncontradoException;
import com.teleticket.cola.repository.PaseCompraRepository;
import com.teleticket.cola.repository.TurnoRepository;
import com.teleticket.cola.repository.VentaEventoRepository;

/**
 * Consulta de un turno: posicion, espera estimada y pase (CU-03, CU-04).
 *
 * El comprador la llama cada pocos segundos mientras espera.
 */
@Service
public class TurnoService {

	private final TurnoRepository turnoRepo;
	private final VentaEventoRepository ventaRepo;
	private final PaseCompraRepository paseRepo;
	private final ColaProperties props;

	public TurnoService(TurnoRepository turnoRepo, VentaEventoRepository ventaRepo,
			PaseCompraRepository paseRepo, ColaProperties props) {
		this.turnoRepo = turnoRepo;
		this.ventaRepo = ventaRepo;
		this.paseRepo = paseRepo;
		this.props = props;
	}

	/**
	 * El turno de quien lo pide. Si el turno es de otra persona responde 404, igual
	 * que si no existiera: no se revela que ese id es real.
	 */
	@Transactional(readOnly = true)
	public TurnoResponse consultar(UUID turnoId, UUID usuarioId) {
		Turno turno = turnoRepo.findById(turnoId)
				.filter(t -> t.getUsuarioId().equals(usuarioId))
				.orElseThrow(() -> new NoEncontradoException("No existe el turno " + turnoId));
		return vista(turno);
	}

	/** Arma lo que ve el comprador segun el estado de su turno. */
	@Transactional(readOnly = true)
	public TurnoResponse vista(Turno turno) {
		VentaEvento venta = ventaRepo.findById(turno.getEventoId()).orElseThrow();

		Long delante = null;
		Long espera = null;
		if (turno.getEstado() == EstadoTurno.EN_COLA) {
			delante = turnoRepo.countByEventoIdAndEstadoAndPosicionLessThan(
					turno.getEventoId(), EstadoTurno.EN_COLA, turno.getPosicion());
			// Cada ciclo habilita 'capacidad' turnos: los de adelante se van por grupos y
			// el ciclo en que sales es el siguiente a todos esos grupos.
			long ciclos = delante / venta.getCapacidadPorCiclo() + 1;
			espera = ciclos * props.cicloSegundos();
		}

		PaseResponse pase = null;
		if (turno.getEstado() == EstadoTurno.HABILITADO || turno.getEstado() == EstadoTurno.EXPIRADO) {
			pase = paseRepo.findByTurnoId(turno.getId()).map(PaseResponse::de).orElse(null);
		}

		return new TurnoResponse(turno.getId(), turno.getEventoId(), turno.getEstado().name(),
				turno.getPosicion(), delante, espera, venta.getInicioVenta(), pase);
	}
}
