package com.teleticket.cola.listener;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;
import com.teleticket.cola.exception.MensajeInvalidoException;
import com.teleticket.cola.service.EventoVentaService;
import com.teleticket.mensajeria.Mensajeria;
import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.EventoPublicado;

@Component
public class EventoPublicadoListener {

	private static final Logger log = LoggerFactory.getLogger(EventoPublicadoListener.class);

	private final EventoVentaService ventaService;

	public EventoPublicadoListener(EventoVentaService ventaService) {
		this.ventaService = ventaService;
	}

	@RabbitListener(queues = Mensajeria.COLA_EVENTO_PUBLICADO)
	public void alRecibir(SobreMensaje<EventoPublicado> sobre, Channel canal,
			@Header(AmqpHeaders.DELIVERY_TAG) long entrega) throws IOException {
		try {
			boolean nuevo = ventaService.registrarEvento(sobre);
			canal.basicAck(entrega, false);
			log.info("evento.publicado {} {}", sobre.eventoId(), nuevo ? "procesado" : "descartado (repetido)");

		} catch (MensajeInvalidoException e) {
			log.error("Mensaje invalido, se descarta: {}", e.getMessage());
			canal.basicReject(entrega, false);

		} catch (Exception e) {
			log.error("No se pudo procesar el mensaje, vuelve a la cola: {}", e.toString());
			canal.basicNack(entrega, false, true);
		}
	}
}
