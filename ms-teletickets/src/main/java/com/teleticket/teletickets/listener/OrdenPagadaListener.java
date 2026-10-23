package com.teleticket.teletickets.listener;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;
import com.teleticket.mensajeria.Mensajeria;
import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.OrdenPagada;
import com.teleticket.teletickets.exception.MensajeInvalidoException;
import com.teleticket.teletickets.service.EmisionService;

@Component
public class OrdenPagadaListener {

	private static final Logger log = LoggerFactory.getLogger(OrdenPagadaListener.class);

	private final EmisionService emisionService;
	private final int reintentos;
	private final long esperaInicialMs;

	public OrdenPagadaListener(EmisionService emisionService,
			@Value("${teleticket.tickets.reintentos}") int reintentos,
			@Value("${teleticket.tickets.espera-inicial-ms}") long esperaInicialMs) {
		this.emisionService = emisionService;
		this.reintentos = reintentos;
		this.esperaInicialMs = esperaInicialMs;
	}

	@RabbitListener(queues = Mensajeria.TICKETS_ORDEN_PAGADA)
	public void alRecibir(SobreMensaje<OrdenPagada> sobre, Channel canal,
			@Header(AmqpHeaders.DELIVERY_TAG) long entrega) throws IOException {
		try {
			boolean emitido = emitirConReintentos(sobre);
			canal.basicAck(entrega, false);
			log.info("orden.pagada {} {}", sobre.eventoId(), emitido ? "procesado" : "sin cambios");

		} catch (MensajeInvalidoException e) {
			log.error("Mensaje invalido, va a la cola de mensajes muertos: {}", e.getMessage());
			canal.basicReject(entrega, false);

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("Apagado durante la emision: el mensaje {} vuelve a la cola", sobre.eventoId());
			canal.basicNack(entrega, false, true);

		} catch (Exception e) {
			log.error("EMISION FALLIDA tras {} reintentos, el mensaje {} va a la cola de mensajes muertos: {}", reintentos,
					sobre != null ? sobre.eventoId() : null, e.toString());
			canal.basicReject(entrega, false);
		}
	}

	private boolean emitirConReintentos(SobreMensaje<OrdenPagada> sobre) throws InterruptedException {
		for (int intento = 0;; intento++) {
			try {
				return emisionService.emitir(sobre);
			} catch (MensajeInvalidoException e) {
				throw e;
			} catch (RuntimeException e) {
				if (intento >= reintentos) {
					throw e;
				}
				long espera = esperaInicialMs << intento;
				log.warn("Emision fallida (intento {} de {}), reintento en {} ms: {}", intento + 1, reintentos + 1,
						espera, e.toString());
				Thread.sleep(espera);
			}
		}
	}
}
