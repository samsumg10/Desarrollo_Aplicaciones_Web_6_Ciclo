package com.teleticket.inventario.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teleticket.inventario.entity.Reserva;
import com.teleticket.inventario.exception.MensajeInvalidoException;
import com.teleticket.inventario.repository.ReservaRepository;
import com.teleticket.inventario.repository.StockZonaRepository;
import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.OrdenReembolsada;

/**
 * Devuelve el stock de una orden reembolsada (fase 11): la compensacion de la reserva de la fase 6.
 *
 * Marca la reserva LIBERADA y repone sus entradas en la zona, en UNA transaccion: las dos cosas o ninguna.
 * Se repone la cantidad que tiene la reserva en esta base de datos y no la del mensaje: es la misma que
 * se desconto al reservar.
 *
 * Idempotente sin tabla de mensajes: la reposicion solo ocurre si el UPDATE de liberar cambio una fila, es
 * decir, la primera vez. Un orden.reembolsada repetido (o dos a la vez) encuentra la reserva ya LIBERADA y
 * no repone nada.
 */
@Service
public class DevolucionService {

	private static final Logger log = LoggerFactory.getLogger(DevolucionService.class);

	private final ReservaRepository reservaRepo;
	private final StockZonaRepository stockRepo;

	public DevolucionService(ReservaRepository reservaRepo, StockZonaRepository stockRepo) {
		this.reservaRepo = reservaRepo;
		this.stockRepo = stockRepo;
	}

	/**
	 * @return true si este mensaje devolvio las entradas al stock, false si la reserva ya estaba LIBERADA
	 * @throws MensajeInvalidoException si el mensaje no trae la reserva o la reserva no existe
	 */
	@Transactional
	public boolean devolverStock(SobreMensaje<OrdenReembolsada> sobre) {
		if (sobre == null || sobre.datos() == null || sobre.datos().ordenId() == null
				|| sobre.datos().reservaId() == null) {
			throw new MensajeInvalidoException("El mensaje no trae la orden y la reserva");
		}
		OrdenReembolsada reembolso = sobre.datos();

		Reserva reserva = reservaRepo.findById(reembolso.reservaId()).orElseThrow(() -> new MensajeInvalidoException(
				"orden.reembolsada de la orden %s apunta a la reserva %s, que no existe"
						.formatted(reembolso.ordenId(), reembolso.reservaId())));

		if (reservaRepo.liberar(reserva.getId()) == 0) {
			log.info("La reserva {} de la orden {} ya estaba LIBERADA: no se repone stock", reserva.getId(),
					reembolso.ordenId());
			return false;
		}

		stockRepo.reponer(reserva.getZonaId(), reserva.getCantidad());
		log.info("Reserva {} de la orden {} LIBERADA: {} entradas vuelven a la zona {}", reserva.getId(),
				reembolso.ordenId(), reserva.getCantidad(), reserva.getZonaId());
		return true;
	}
}
