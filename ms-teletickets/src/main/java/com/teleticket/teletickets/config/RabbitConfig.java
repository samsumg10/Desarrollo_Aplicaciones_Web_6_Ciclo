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

	@Bean
	Queue colaOrdenesTicketsEmitidos() {
		return QueueBuilder.durable(Mensajeria.ORDENES_TICKETS_EMITIDOS).build();
	}

	@Bean
	Binding bindingOrdenesTicketsEmitidos(@Qualifier("colaOrdenesTicketsEmitidos") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.TICKETS_EMITIDOS);
	}

	@Bean
	Queue colaTicketsOrdenPagadaDlq() {
		return QueueBuilder.durable(Mensajeria.TICKETS_ORDEN_PAGADA_DLQ).build();
	}

	@Bean
	Binding bindingTicketsOrdenPagadaDlq(@Qualifier("colaTicketsOrdenPagadaDlq") Queue cola,
			@Qualifier("exchangeMuertos") TopicExchange dlx) {
		return BindingBuilder.bind(cola).to(dlx).with(Mensajeria.TICKETS_ORDEN_PAGADA_DLQ);
	}

	@Bean
	Queue colaOrdenesEmisionFallida() {
		return QueueBuilder.durable(Mensajeria.ORDENES_EMISION_FALLIDA).build();
	}

	@Bean
	Binding bindingOrdenesEmisionFallida(@Qualifier("colaOrdenesEmisionFallida") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.EMISION_FALLIDA);
	}

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
