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

/**
 * Recibe orden.pagada y emite las entradas, con reintentos de espera creciente (RN-08).
 *
 * Mismo patron de ack manual que ms-inventario: el mensaje se confirma solo DESPUES de emitir y anunciar.
 *
 * Reintentos: si la emision falla se vuelve a intentar hasta {@code reintentos} veces, esperando
 * 1 s, 2 s, 4 s (la espera se duplica). Los reintentos los hace el propio listener y no un
 * requeue en RabbitMQ: un requeue devolveria el mensaje al instante, sin espera, y lo martillaria contra
 * un ms-eventos que quizas esta arrancando.
 *
 * Agotados los reintentos el mensaje se descarta con un ERROR en el log. La orden queda PAGADA sin entradas:
 * la fase 11 manda ese mensaje a una cola de mensajes muertos y reembolsa. Es el unico punto que cambia alli.
 */
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
			// Reintentarlo no lo arregla. Se descarta (requeue=false) para no entrar en un bucle.
			log.error("Mensaje invalido, se descarta: {}", e.getMessage());
			canal.basicReject(entrega, false);

		} catch (InterruptedException e) {
			// El servicio se esta apagando. Sin ack, RabbitMQ devuelve el mensaje y se emite al volver.
			Thread.currentThread().interrupt();
			log.warn("Apagado durante la emision: el mensaje {} vuelve a la cola", sobre.eventoId());
			canal.basicNack(entrega, false, true);

		} catch (Exception e) {
			// Sin reintentos: queda PAGADA sin entradas. Aqui engancha la fase 11 (cola de mensajes muertos).
			log.error("EMISION FALLIDA tras {} reintentos, se descarta el mensaje {}: {}", reintentos,
					sobre != null ? sobre.eventoId() : null, e.toString());
			canal.basicReject(entrega, false);
		}
	}

	/** Intenta emitir; si falla, espera y reintenta. La ultima excepcion se propaga. */
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
