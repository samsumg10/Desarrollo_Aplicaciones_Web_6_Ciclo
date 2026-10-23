package com.teleticket.ordenes.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.TicketsEmitidos;
import com.teleticket.ordenes.entity.EstadoOrden;
import com.teleticket.ordenes.entity.OrdenHistorial;
import com.teleticket.ordenes.exception.MensajeInvalidoException;
import com.teleticket.ordenes.repository.OrdenHistorialRepository;
import com.teleticket.ordenes.repository.OrdenRepository;

@Service
public class EmisionService {

	private static final Logger log = LoggerFactory.getLogger(EmisionService.class);

	private final OrdenRepository ordenRepo;
	private final OrdenHistorialRepository historialRepo;

	public EmisionService(OrdenRepository ordenRepo, OrdenHistorialRepository historialRepo) {
		this.ordenRepo = ordenRepo;
		this.historialRepo = historialRepo;
	}

	@Transactional
	public boolean marcarEmitida(SobreMensaje<TicketsEmitidos> sobre) {
		if (sobre == null || sobre.datos() == null || sobre.datos().ordenId() == null) {
			throw new MensajeInvalidoException("El mensaje no trae la orden");
		}
		TicketsEmitidos emitidos = sobre.datos();
		UUID ordenId = emitidos.ordenId();

		EstadoOrden estado = ordenRepo.findById(ordenId)
				.orElseThrow(() -> new MensajeInvalidoException(
						"tickets.emitidos apunta a la orden %s, que no existe".formatted(ordenId)))
				.getEstado();

		if (!estado.puedePasarA(EstadoOrden.EMITIDA)) {
			if (estado == EstadoOrden.EMITIDA) {
				log.info("La orden {} ya estaba EMITIDA: sin cambios", ordenId);
			} else {
				log.warn("tickets.emitidos de la orden {}, pero esta {} y no puede pasar a EMITIDA: no se cambia",
						ordenId, estado);
			}
			return false;
		}

		if (ordenRepo.cambiarEstado(ordenId, EstadoOrden.PAGADA, EstadoOrden.EMITIDA) == 1) {
			int cantidad = emitidos.ticketIds() != null ? emitidos.ticketIds().size() : 0;
			historialRepo.save(OrdenHistorial.registrar(ordenId, EstadoOrden.PAGADA, EstadoOrden.EMITIDA,
					"%d entradas emitidas con firma digital".formatted(cantidad)));
			log.info("Orden {} EMITIDA ({} entradas)", ordenId, cantidad);
			return true;
		}

		if (estado == EstadoOrden.EMITIDA) {
			log.info("La orden {} ya estaba EMITIDA: sin cambios", ordenId);
		} else {
			log.warn("tickets.emitidos de la orden {}, pero esta {} y no PAGADA: no se cambia", ordenId, estado);
		}
		return false;
	}
}
