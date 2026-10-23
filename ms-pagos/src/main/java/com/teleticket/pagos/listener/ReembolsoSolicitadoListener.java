package com.teleticket.pagos.listener;

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
import com.teleticket.mensajeria.mensajes.ReembolsoSolicitado;
import com.teleticket.pagos.exception.MensajeInvalidoException;
import com.teleticket.pagos.service.ReembolsoService;

/**
 * Recibe reembolso.solicitado y devuelve el dinero (fase 11). Mismo patron de ack manual que los demas
 * listeners: el mensaje se confirma solo despues de guardar el reembolso y de que pago.reembolsado quede publicado.
 */
@Component
public class ReembolsoSolicitadoListener {

	private static final Logger log = LoggerFactory.getLogger(ReembolsoSolicitadoListener.class);

	private final ReembolsoService reembolsoService;

	public ReembolsoSolicitadoListener(ReembolsoService reembolsoService) {
		this.reembolsoService = reembolsoService;
	}

	@RabbitListener(queues = Mensajeria.PAGOS_REEMBOLSO_SOLICITADO)
	public void alRecibir(SobreMensaje<ReembolsoSolicitado> sobre, Channel canal,
			@Header(AmqpHeaders.DELIVERY_TAG) long entrega) throws IOException {
		try {
			boolean devuelto = reembolsoService.reembolsar(sobre);
			canal.basicAck(entrega, false);
			log.info("reembolso.solicitado {} {}", sobre.eventoId(), devuelto ? "procesado" : "sin cambios");

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
