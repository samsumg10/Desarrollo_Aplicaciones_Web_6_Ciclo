package com.teleticket.teletickets.listener;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;
import com.teleticket.mensajeria.Mensajeria;
import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.OrdenReembolsada;
import com.teleticket.teletickets.exception.MensajeInvalidoException;
import com.teleticket.teletickets.service.AnulacionService;

/**
 * Recibe orden.reembolsada y anula las entradas de esa orden. Mismo patron de ack manual que los demas
 * listeners: primero se guarda y solo despues se confirma a RabbitMQ.
 */
@Component
public class OrdenReembolsadaListener {

	private static final Logger log = LoggerFactory.getLogger(OrdenReembolsadaListener.class);

	private final AnulacionService anulacionService;

	public OrdenReembolsadaListener(AnulacionService anulacionService) {
		this.anulacionService = anulacionService;
	}

	@RabbitListener(queues = Mensajeria.TICKETS_ORDEN_REEMBOLSADA)
	public void alRecibir(SobreMensaje<OrdenReembolsada> sobre, Channel canal,
			@Header(AmqpHeaders.DELIVERY_TAG) long entrega) throws IOException {
		try {
			int anuladas = anulacionService.anular(sobre);
			canal.basicAck(entrega, false);
			log.info("orden.reembolsada {} procesado ({} anuladas)", sobre.eventoId(), anuladas);

		} catch (MensajeInvalidoException e) {
			// Reintentarlo no lo arregla. Se descarta (requeue=false) para no entrar en un bucle.
			log.error("Mensaje invalido, se descarta: {}", e.getMessage());
			canal.basicReject(entrega, false);

		} catch (Exception e) {
			// Fallo pasajero (base de datos caida, por ejemplo): vuelve a la cola para reintentarlo.
			log.error("No se pudo procesar el mensaje, vuelve a la cola: {}", e.toString());
			canal.basicNack(entrega, false, true);
		}
	}
}
