package com.teleticket.eventos.config;

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
	Queue colaEventoPublicado() {
		return QueueBuilder.durable(Mensajeria.COLA_EVENTO_PUBLICADO).build();
	}

	@Bean
	Queue colaInventarioEventoPublicado() {
		return QueueBuilder.durable(Mensajeria.INVENTARIO_EVENTO_PUBLICADO).build();
	}

	@Bean
	Binding bindingCola(@Qualifier("colaEventoPublicado") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.EVENTO_PUBLICADO);
	}

	@Bean
	Binding bindingInventario(@Qualifier("colaInventarioEventoPublicado") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.EVENTO_PUBLICADO);
	}
}
