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
import com.teleticket.mensajeria.PublicadorEventos;
import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.EmisionFallida;
import com.teleticket.mensajeria.mensajes.OrdenPagada;

/**
 * Lee la cola de mensajes muertos de orden.pagada y publica emision.fallida: el primer paso de la saga (RN-08).
 *
 * Un orden.pagada llega aqui cuando la emision agoto sus reintentos. Este listener no reembolsa nada:
 * solo avisa a ms-ordenes, que es quien decide sobre la orden. El mensaje se confirma (ack) DESPUES de que el
 * broker confirme la publicacion; si ese momento se cae, el mensaje sigue en la DLQ y se anuncia otra vez
 * (ms-ordenes tolera el aviso repetido).
 */
@Component
public class EmisionFallidaListener {

	private static final Logger log = LoggerFactory.getLogger(EmisionFallidaListener.class);

	private final PublicadorEventos publicador;
	private final int reintentos;

	public EmisionFallidaListener(PublicadorEventos publicador,
			@Value("${teleticket.tickets.reintentos}") int reintentos) {
		this.publicador = publicador;
		this.reintentos = reintentos;
	}

	@RabbitListener(queues = Mensajeria.TICKETS_ORDEN_PAGADA_DLQ)
	public void alRecibir(SobreMensaje<OrdenPagada> sobre, Channel canal,
			@Header(AmqpHeaders.DELIVERY_TAG) long entrega) throws IOException {
		OrdenPagada orden = sobre != null ? sobre.datos() : null;

		if (orden == null || orden.ordenId() == null) {
			// Sin orden no hay a quien reembolsar. Se confirma para sacarlo de la DLQ y se deja el rastro en el log.
			log.error("Mensaje muerto sin orden, no se puede iniciar el reembolso: {}", sobre);
			canal.basicAck(entrega, false);
			return;
		}

		try {
			String motivo = "No se pudieron emitir las entradas tras %d intentos".formatted(reintentos + 1);
			publicador.publicar(Mensajeria.EMISION_FALLIDA, SobreMensaje.de(Mensajeria.EMISION_FALLIDA,
					sobre.correlationId(), new EmisionFallida(orden.ordenId(), motivo)));
			canal.basicAck(entrega, false);
			log.warn("Orden {}: emision fallida, se publico emision.fallida para reembolsarla", orden.ordenId());

		} catch (Exception e) {
			// El broker no confirmo: el mensaje vuelve a la DLQ y se reintenta.
			log.error("No se pudo publicar emision.fallida de la orden {}, vuelve a la cola: {}", orden.ordenId(),
					e.toString());
			canal.basicNack(entrega, false, true);
		}
	}
}
