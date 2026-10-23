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

/**
 * Reservas de entradas y consulta de disponibilidad (CU-05, parte de stock).
 *
 * Lo llama ms-ordenes por Feign (fase 8). Aqui no hay reglas de precio ni de
 * limite por comprador: este servicio solo responde por el stock.
 */
@Service
public class ReservaService {

	private final StockZonaRepository stockRepo;
	private final ReservaRepository reservaRepo;

	public ReservaService(StockZonaRepository stockRepo, ReservaRepository reservaRepo) {
		this.stockRepo = stockRepo;
		this.reservaRepo = reservaRepo;
	}

	/**
	 * Aparta N entradas de una zona para una orden.
	 *
	 * Es idempotente por orden: si ms-ordenes reintenta la llamada (por ejemplo,
	 * porque se agoto el tiempo de espera de Feign) y la reserva ya existe con
	 * los mismos datos, devuelve la misma sin apartar entradas otra vez.
	 *
	 * Todo ocurre en una transaccion: si algo falla despues de descontar el
	 * stock, el descuento tambien se deshace.
	 */
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

		// La consulta atomica: resta solo si alcanza. 1 = reservado, 0 = no.
		if (stockRepo.descontar(req.zonaId(), req.cantidad()) == 0) {
			// 0 filas puede ser "no alcanza" o "la zona no existe": se distingue para dar el error correcto.
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
			// Dos peticiones simultaneas de la misma orden pasaron la comprobacion de arriba a la
			// vez; UNIQUE(orden_id) rechazo a la segunda. Al lanzar la excepcion, la transaccion
			// se deshace y el stock que acabamos de descontar vuelve.
			throw new ReglaNegocioException("RESERVA_DUPLICADA",
					"La orden %s ya tiene una reserva en curso".formatted(req.ordenId()));
		}
	}

	/** ACTIVA -> EN_PAGO: protege la reserva mientras se cobra. Rechaza las ya vencidas. */
	@Transactional
	public ReservaResponse marcarEnPago(UUID reservaId) {
		return transicion(reservaId, EstadoReserva.ACTIVA, EstadoReserva.EN_PAGO, true);
	}

	/**
	 * EN_PAGO -> ACTIVA: el pago se rechazo y el comprador puede reintentar.
	 *
	 * No se comprueba el vencimiento: si la reserva vencio mientras se cobraba,
	 * vuelve a ACTIVA y el proceso de vencimiento la liberara.
	 */
	@Transactional
	public ReservaResponse activar(UUID reservaId) {
		return transicion(reservaId, EstadoReserva.EN_PAGO, EstadoReserva.ACTIVA, false);
	}

	/** Cuantas entradas quedan en la zona. */
	@Transactional(readOnly = true)
	public int disponible(UUID zonaId) {
		return stockRepo.findById(zonaId)
				.orElseThrow(() -> new NoEncontradoException("No hay stock para la zona " + zonaId))
				.getDisponible();
	}

	// -----------------------------------------------------------------

	/**
	 * Mueve la reserva de un estado a otro.
	 *
	 * Si ya esta en el estado de destino devuelve OK sin hacer nada: asi
	 * ms-ordenes puede repetir la llamada si no recibio la respuesta.
	 */
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

		// UPDATE condicional: solo cambia si sigue en 'desde'. 0 = otra operacion se adelanto.
		if (reservaRepo.cambiarEstado(id, desde, hacia) == 0) {
			throw new ReglaNegocioException("ESTADO_INVALIDO",
					"La reserva cambio de estado mientras se procesaba. Consulta su estado e intenta de nuevo");
		}
		return ReservaResponse.de(reservaRepo.findById(id).orElseThrow());
	}
}
