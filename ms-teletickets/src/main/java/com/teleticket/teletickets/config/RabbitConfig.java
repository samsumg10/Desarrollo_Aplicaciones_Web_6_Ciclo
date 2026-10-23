package com.teleticket.teletickets.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.teleticket.mensajeria.Mensajeria;

/**
 * Las colas de ms-tickets: la suya (tickets.orden-pagada) y la de su consumidor (ordenes.tickets-emitidos).
 *
 * Cada una se declara tambien en el otro extremo (ms-ordenes), con los mismos parametros. Asi el mensaje
 * tiene donde esperar aunque el servicio que lo va a leer este apagado: con template.mandatory=true, un
 * mensaje que no llega a ninguna cola hace fallar la publicacion.
 *
 * Si algun dia se le agrega un argumento a una cola (la fase 11 le pone un DLX a tickets.orden-pagada),
 * hay que cambiarlo en los dos sitios a la vez: RabbitMQ rechaza redeclarar una cola con parametros
 * distintos (PRECONDITION_FAILED).
 */
@Configuration
public class RabbitConfig {

	@Bean
	Queue colaTicketsOrdenPagada() {
		return QueueBuilder.durable(Mensajeria.TICKETS_ORDEN_PAGADA).build();
	}

	@Bean
	Binding bindingTicketsOrdenPagada(@Qualifier("colaTicketsOrdenPagada") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.ORDEN_PAGADA);
	}

	/** Donde esperan los tickets.emitidos que ms-tickets publica y ms-ordenes consume. */
	@Bean
	Queue colaOrdenesTicketsEmitidos() {
		return QueueBuilder.durable(Mensajeria.ORDENES_TICKETS_EMITIDOS).build();
	}

	@Bean
	Binding bindingOrdenesTicketsEmitidos(@Qualifier("colaOrdenesTicketsEmitidos") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.TICKETS_EMITIDOS);
	}
}
