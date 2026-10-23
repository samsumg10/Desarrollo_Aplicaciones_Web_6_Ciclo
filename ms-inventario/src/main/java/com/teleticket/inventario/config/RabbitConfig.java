package com.teleticket.inventario.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.teleticket.mensajeria.Mensajeria;

@Configuration
public class RabbitConfig {

	@Bean
	Queue colaInventarioEventoPublicado() {
		return QueueBuilder.durable(Mensajeria.INVENTARIO_EVENTO_PUBLICADO).build();
	}

	@Bean
	Binding bindingEventoPublicado(@Qualifier("colaInventarioEventoPublicado") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.EVENTO_PUBLICADO);
	}

	@Bean
	Queue colaInventarioOrdenPagada() {
		return QueueBuilder.durable(Mensajeria.INVENTARIO_ORDEN_PAGADA).build();
	}

	@Bean
	Binding bindingOrdenPagada(@Qualifier("colaInventarioOrdenPagada") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.ORDEN_PAGADA);
	}

	@Bean
	Queue colaInventarioOrdenReembolsada() {
		return QueueBuilder.durable(Mensajeria.INVENTARIO_ORDEN_REEMBOLSADA).build();
	}

	@Bean
	Binding bindingOrdenReembolsada(@Qualifier("colaInventarioOrdenReembolsada") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.ORDEN_REEMBOLSADA);
	}
}
