package com.teleticket.inventario.service;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teleticket.inventario.dto.CrearReservaRequest;
import com.teleticket.inventario.dto.ReservaResponse;
import com.teleticket.inventario.entity.EstadoReserva;
import com.teleticket.inventario.entity.Reserva;
import com.teleticket.inventario.exception.NoEncontradoException;
import com.teleticket.inventario.exception.ReglaNegocioException;
import com.teleticket.inventario.repository.ReservaRepository;
import com.teleticket.inventario.repository.StockZonaRepository;

@Service
public class ReservaService {

	private final StockZonaRepository stockRepo;
	private final ReservaRepository reservaRepo;

	public ReservaService(StockZonaRepository stockRepo, ReservaRepository reservaRepo) {
		this.stockRepo = stockRepo;
		this.reservaRepo = reservaRepo;
	}

	@Transactional
	public ReservaResponse reservar(CrearReservaRequest req) {
		var existente = reservaRepo.findByOrdenId(req.ordenId());
		if (existente.isPresent()) {
			Reserva r = existente.get();
			if (r.getZonaId().equals(req.zonaId()) && r.getCantidad().equals(req.cantidad())) {
				return ReservaResponse.de(r);
			}
			throw new ReglaNegocioException("ORDEN_YA_TIENE_RESERVA",
					"La orden %s ya tiene la reserva %s con otra zona o cantidad"
							.formatted(req.ordenId(), r.getId()));
		}

		if (stockRepo.descontar(req.zonaId(), req.cantidad()) == 0) {
			if (!stockRepo.existsById(req.zonaId())) {
				throw new NoEncontradoException("No hay stock para la zona " + req.zonaId()
						+ ": el evento no se ha publicado o la zona no existe");
			}
			throw new ReglaNegocioException("SIN_STOCK", "Sin stock suficiente");
		}

		try {
			return ReservaResponse.de(reservaRepo.saveAndFlush(
					Reserva.crear(req.ordenId(), req.zonaId(), req.cantidad())));
		} catch (DataIntegrityViolationException e) {
			throw new ReglaNegocioException("RESERVA_DUPLICADA",
					"La orden %s ya tiene una reserva en curso".formatted(req.ordenId()));
		}
	}

	@Transactional
	public ReservaResponse marcarEnPago(UUID reservaId) {
		return transicion(reservaId, EstadoReserva.ACTIVA, EstadoReserva.EN_PAGO, true);
	}

	@Transactional
	public ReservaResponse activar(UUID reservaId) {
		return transicion(reservaId, EstadoReserva.EN_PAGO, EstadoReserva.ACTIVA, false);
	}

	@Transactional(readOnly = true)
	public int disponible(UUID zonaId) {
		return stockRepo.findById(zonaId)
				.orElseThrow(() -> new NoEncontradoException("No hay stock para la zona " + zonaId))
				.getDisponible();
	}

	private ReservaResponse transicion(UUID id, EstadoReserva desde, EstadoReserva hacia,
			boolean exigirVigente) {
		Reserva r = reservaRepo.findById(id)
				.orElseThrow(() -> new NoEncontradoException("No existe la reserva " + id));

		if (r.getEstado() == hacia) {
			return ReservaResponse.de(r);
		}
		if (r.getEstado() != desde) {
			throw new ReglaNegocioException("ESTADO_INVALIDO",
					"La reserva esta %s y solo se puede pasar a %s desde %s"
							.formatted(r.getEstado(), hacia, desde));
		}
		if (exigirVigente && r.haVencido()) {
			throw new ReglaNegocioException("RESERVA_VENCIDA",
					"La reserva vencio el " + r.getVenceEn());
		}

		if (reservaRepo.cambiarEstado(id, desde, hacia) == 0) {
			throw new ReglaNegocioException("ESTADO_INVALIDO",
					"La reserva cambio de estado mientras se procesaba. Consulta su estado e intenta de nuevo");
		}
		return ReservaResponse.de(reservaRepo.findById(id).orElseThrow());
	}
}
