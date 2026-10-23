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

@Service
public class DevolucionService {

	private static final Logger log = LoggerFactory.getLogger(DevolucionService.class);

	private final ReservaRepository reservaRepo;
	private final StockZonaRepository stockRepo;

	public DevolucionService(ReservaRepository reservaRepo, StockZonaRepository stockRepo) {
		this.reservaRepo = reservaRepo;
		this.stockRepo = stockRepo;
	}

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
