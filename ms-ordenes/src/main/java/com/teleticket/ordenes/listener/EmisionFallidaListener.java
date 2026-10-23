package com.teleticket.ordenes.listener;

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
import com.teleticket.mensajeria.mensajes.EmisionFallida;
import com.teleticket.ordenes.exception.MensajeInvalidoException;
import com.teleticket.ordenes.service.ReembolsoService;

@Component
public class EmisionFallidaListener {

	private static final Logger log = LoggerFactory.getLogger(EmisionFallidaListener.class);

	private final ReembolsoService reembolsoService;

	public EmisionFallidaListener(ReembolsoService reembolsoService) {
		this.reembolsoService = reembolsoService;
	}

	@RabbitListener(queues = Mensajeria.ORDENES_EMISION_FALLIDA)
	public void alRecibir(SobreMensaje<EmisionFallida> sobre, Channel canal,
			@Header(AmqpHeaders.DELIVERY_TAG) long entrega) throws IOException {
		try {
			boolean solicitado = reembolsoService.solicitarReembolso(sobre);
			canal.basicAck(entrega, false);
			log.info("emision.fallida {} {}", sobre.eventoId(), solicitado ? "procesado" : "sin cambios");

		} catch (MensajeInvalidoException e) {
			log.error("Mensaje invalido, se descarta: {}", e.getMessage());
			canal.basicReject(entrega, false);

		} catch (Exception e) {
			log.error("No se pudo procesar el mensaje, vuelve a la cola: {}", e.toString());
			canal.basicNack(entrega, false, true);
		}
	}
}
