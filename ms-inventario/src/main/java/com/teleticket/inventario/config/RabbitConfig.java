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

/**
 * Las colas de ms-inventario: evento.publicado (fase 6), orden.pagada (fase 9) y orden.reembolsada (fase 11).
 *
 * Cada consumidor declara SU cola: asi el servicio arranca aunque ms-eventos
 * no lo haya hecho antes. ms-eventos la declara tambien (fase 5, para poder ver
 * los mensajes acumulados antes de que existiera este servicio); no hay conflicto
 * porque ambos la declaran con los mismos parametros.
 *
 * Si algun dia se le agrega un argumento (por ejemplo un DLX), hay que cambiarlo
 * en los dos sitios a la vez: RabbitMQ rechaza redeclarar una cola con
 * parametros distintos (PRECONDITION_FAILED).
 */
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

	/** Fase 9: orden.pagada. ms-ordenes declara la misma cola (ver su RabbitConfig); mismos parametros. */
	@Bean
	Queue colaInventarioOrdenPagada() {
		return QueueBuilder.durable(Mensajeria.INVENTARIO_ORDEN_PAGADA).build();
	}

	@Bean
	Binding bindingOrdenPagada(@Qualifier("colaInventarioOrdenPagada") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.ORDEN_PAGADA);
	}

	/** Fase 11: orden.reembolsada devuelve el stock. ms-ordenes declara la misma cola (la publica); mismos parametros. */
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
