package com.teleticket.inventario.listener;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;
import com.teleticket.inventario.exception.MensajeInvalidoException;
import com.teleticket.inventario.service.DevolucionService;
import com.teleticket.mensajeria.Mensajeria;
import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.OrdenReembolsada;

/**
 * Recibe orden.reembolsada y devuelve el stock (fase 11). Mismo patron de ack manual que OrdenPagadaListener:
 * primero se guarda en la base de datos y solo despues se confirma a RabbitMQ.
 */
@Component
public class OrdenReembolsadaListener {

	private static final Logger log = LoggerFactory.getLogger(OrdenReembolsadaListener.class);

	private final DevolucionService devolucionService;

	public OrdenReembolsadaListener(DevolucionService devolucionService) {
		this.devolucionService = devolucionService;
	}

	@RabbitListener(queues = Mensajeria.INVENTARIO_ORDEN_REEMBOLSADA)
	public void alRecibir(SobreMensaje<OrdenReembolsada> sobre, Channel canal,
			@Header(AmqpHeaders.DELIVERY_TAG) long entrega) throws IOException {
		try {
			boolean devuelto = devolucionService.devolverStock(sobre);
			canal.basicAck(entrega, false);
			log.info("orden.reembolsada {} {}", sobre.eventoId(), devuelto ? "procesado" : "sin cambios");

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
