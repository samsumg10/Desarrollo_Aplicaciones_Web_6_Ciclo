package com.teleticket.mensajeria;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;

/**
 * Colas que declaran dos servicios a la vez y que llevan argumentos.
 *
 * RabbitMQ rechaza redeclarar una cola con parametros distintos (PRECONDITION_FAILED), asi que las
 * colas con argumentos se construyen aqui, en un solo sitio, y los servicios solo las exponen como bean.
 * Las colas sin argumentos siguen declarandose con QueueBuilder.durable(...) en cada servicio.
 */
public final class ColasCompartidas {

	private ColasCompartidas() {
	}

	/**
	 * tickets.orden-pagada con su cola de mensajes muertos (fase 11).
	 *
	 * La declaran ms-tickets (la consume) y ms-ordenes (publica orden.pagada y no debe depender de que
	 * ms-tickets haya arrancado). Cuando el listener rechaza un mensaje sin devolverlo a la cola, RabbitMQ
	 * lo reenvia al DLX con la routing key {@link Mensajeria#TICKETS_ORDEN_PAGADA_DLQ}, y de ahi llega a
	 * la cola del mismo nombre.
	 */
	public static Queue ticketsOrdenPagada() {
		return QueueBuilder.durable(Mensajeria.TICKETS_ORDEN_PAGADA)
				.deadLetterExchange(Mensajeria.DLX)
				.deadLetterRoutingKey(Mensajeria.TICKETS_ORDEN_PAGADA_DLQ)
				.build();
	}
}
