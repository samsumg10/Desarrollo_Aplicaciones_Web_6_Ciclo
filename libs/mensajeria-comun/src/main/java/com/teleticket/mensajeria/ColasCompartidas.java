package com.teleticket.mensajeria;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;

public final class ColasCompartidas {

	private ColasCompartidas() {
	}

	public static Queue ticketsOrdenPagada() {
		return QueueBuilder.durable(Mensajeria.TICKETS_ORDEN_PAGADA)
				.deadLetterExchange(Mensajeria.DLX)
				.deadLetterRoutingKey(Mensajeria.TICKETS_ORDEN_PAGADA_DLQ)
				.build();
	}
}
