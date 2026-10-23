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
import com.teleticket.mensajeria.mensajes.PagoReembolsado;
import com.teleticket.ordenes.exception.MensajeInvalidoException;
import com.teleticket.ordenes.service.ReembolsoService;

/**
 * Recibe pago.reembolsado, deja la orden REEMBOLSADA y anuncia orden.reembolsada (fase 11). Ack manual: se
 * confirma solo despues de guardar el estado y de que el broker confirme el anuncio.
 */
@Component
public class PagoReembolsadoListener {

	private static final Logger log = LoggerFactory.getLogger(PagoReembolsadoListener.class);

	private final ReembolsoService reembolsoService;

	public PagoReembolsadoListener(ReembolsoService reembolsoService) {
		this.reembolsoService = reembolsoService;
	}

	@RabbitListener(queues = Mensajeria.ORDENES_PAGO_REEMBOLSADO)
	public void alRecibir(SobreMensaje<PagoReembolsado> sobre, Channel canal,
			@Header(AmqpHeaders.DELIVERY_TAG) long entrega) throws IOException {
		try {
			boolean cambio = reembolsoService.marcarReembolsada(sobre);
			canal.basicAck(entrega, false);
			log.info("pago.reembolsado {} {}", sobre.eventoId(), cambio ? "procesado" : "sin cambios");

		} catch (MensajeInvalidoException e) {
			// Reintentarlo no lo arregla. Se descarta (requeue=false) para no entrar en un bucle.
			log.error("Mensaje invalido, se descarta: {}", e.getMessage());
			canal.basicReject(entrega, false);

		} catch (Exception e) {
			// Fallo pasajero (base de datos o broker caidos): vuelve a la cola para reintentarlo.
			log.error("No se pudo procesar el mensaje, vuelve a la cola: {}", e.toString());
			canal.basicNack(entrega, false, true);
		}
	}
}
