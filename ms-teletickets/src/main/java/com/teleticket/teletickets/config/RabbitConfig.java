package com.teleticket.teletickets.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.teleticket.mensajeria.ColasCompartidas;
import com.teleticket.mensajeria.Mensajeria;

/**
 * Las colas de ms-tickets: la suya (tickets.orden-pagada) y la de su consumidor (ordenes.tickets-emitidos).
 *
 * Cada una se declara tambien en el otro extremo (ms-ordenes), con los mismos parametros. Asi el mensaje
 * tiene donde esperar aunque el servicio que lo va a leer este apagado: con template.mandatory=true, un
 * mensaje que no llega a ninguna cola hace fallar la publicacion.
 *
 * Fase 11: tickets.orden-pagada lleva un DLX. Se construye en ColasCompartidas (libs/mensajeria-comun) para que
 * ms-tickets y ms-ordenes la declaren con argumentos identicos; RabbitMQ rechaza redeclarar una cola con
 * parametros distintos (PRECONDITION_FAILED). Una cola que ya existia sin DLX hay que borrarla antes de arrancar.
 *
 * Tambien declara la cola de mensajes muertos, la de ordenes.emision-fallida (la de su consumidor, ms-ordenes)
 * y la suya de orden.reembolsada.
 */
@Configuration
public class RabbitConfig {

	@Bean
	Queue colaTicketsOrdenPagada() {
		return ColasCompartidas.ticketsOrdenPagada();
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

	/** Fase 11: los orden.pagada que agotaron sus reintentos. Se enlaza al DLX con la routing key del mismo nombre. */
	@Bean
	Queue colaTicketsOrdenPagadaDlq() {
		return QueueBuilder.durable(Mensajeria.TICKETS_ORDEN_PAGADA_DLQ).build();
	}

	@Bean
	Binding bindingTicketsOrdenPagadaDlq(@Qualifier("colaTicketsOrdenPagadaDlq") Queue cola,
			@Qualifier("exchangeMuertos") TopicExchange dlx) {
		return BindingBuilder.bind(cola).to(dlx).with(Mensajeria.TICKETS_ORDEN_PAGADA_DLQ);
	}

	/** Fase 11: emision.fallida, que publica ms-tickets y consume ms-ordenes (declarada tambien alla). */
	@Bean
	Queue colaOrdenesEmisionFallida() {
		return QueueBuilder.durable(Mensajeria.ORDENES_EMISION_FALLIDA).build();
	}

	@Bean
	Binding bindingOrdenesEmisionFallida(@Qualifier("colaOrdenesEmisionFallida") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.EMISION_FALLIDA);
	}

	/** Fase 11: orden.reembolsada, para anular las entradas. ms-ordenes la declara tambien. */
	@Bean
	Queue colaTicketsOrdenReembolsada() {
		return QueueBuilder.durable(Mensajeria.TICKETS_ORDEN_REEMBOLSADA).build();
	}

	@Bean
	Binding bindingTicketsOrdenReembolsada(@Qualifier("colaTicketsOrdenReembolsada") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.ORDEN_REEMBOLSADA);
	}
}
