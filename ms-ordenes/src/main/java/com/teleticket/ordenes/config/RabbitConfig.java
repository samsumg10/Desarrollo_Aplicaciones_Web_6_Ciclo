package com.teleticket.ordenes.config;

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
	Queue colaInventarioOrdenPagada() {
		return QueueBuilder.durable(Mensajeria.INVENTARIO_ORDEN_PAGADA).build();
	}

	@Bean
	Binding bindingInventarioOrdenPagada(@Qualifier("colaInventarioOrdenPagada") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.ORDEN_PAGADA);
	}

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
	Queue colaOrdenesEmisionFallida() {
		return QueueBuilder.durable(Mensajeria.ORDENES_EMISION_FALLIDA).build();
	}

	@Bean
	Binding bindingOrdenesEmisionFallida(@Qualifier("colaOrdenesEmisionFallida") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.EMISION_FALLIDA);
	}

	@Bean
	Queue colaOrdenesPagoReembolsado() {
		return QueueBuilder.durable(Mensajeria.ORDENES_PAGO_REEMBOLSADO).build();
	}

	@Bean
	Binding bindingOrdenesPagoReembolsado(@Qualifier("colaOrdenesPagoReembolsado") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.PAGO_REEMBOLSADO);
	}

	@Bean
	Queue colaPagosReembolsoSolicitado() {
		return QueueBuilder.durable(Mensajeria.PAGOS_REEMBOLSO_SOLICITADO).build();
	}

	@Bean
	Binding bindingPagosReembolsoSolicitado(@Qualifier("colaPagosReembolsoSolicitado") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.REEMBOLSO_SOLICITADO);
	}

	@Bean
	Queue colaInventarioOrdenReembolsada() {
		return QueueBuilder.durable(Mensajeria.INVENTARIO_ORDEN_REEMBOLSADA).build();
	}

	@Bean
	Binding bindingInventarioOrdenReembolsada(@Qualifier("colaInventarioOrdenReembolsada") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.ORDEN_REEMBOLSADA);
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
