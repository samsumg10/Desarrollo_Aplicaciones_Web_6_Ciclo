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
import com.teleticket.inventario.service.ConfirmacionService;
import com.teleticket.mensajeria.Mensajeria;
import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.OrdenPagada;

/**
 * Recibe orden.pagada y confirma la reserva. Mismo patron de ack manual que EventoPublicadoListener:
 * primero se guarda en la base de datos y solo despues se confirma a RabbitMQ.
 */
@Component
public class OrdenPagadaListener {

	private static final Logger log = LoggerFactory.getLogger(OrdenPagadaListener.class);

	private final ConfirmacionService confirmacionService;

	public OrdenPagadaListener(ConfirmacionService confirmacionService) {
		this.confirmacionService = confirmacionService;
	}

	@RabbitListener(queues = Mensajeria.INVENTARIO_ORDEN_PAGADA)
	public void alRecibir(SobreMensaje<OrdenPagada> sobre, Channel canal,
			@Header(AmqpHeaders.DELIVERY_TAG) long entrega) throws IOException {
		try {
			boolean confirmada = confirmacionService.confirmarReserva(sobre);
			canal.basicAck(entrega, false);
			log.info("orden.pagada {} {}", sobre.eventoId(), confirmada ? "procesado" : "sin cambios");

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
