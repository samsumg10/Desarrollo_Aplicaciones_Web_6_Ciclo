package com.teleticket.cola.config;

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
 * Las colas de ms-cola.
 *
 * cola.evento-publicado es la suya: la declara aqui aunque ms-eventos tambien lo
 * hace (fase 5), con los mismos parametros.
 *
 * notificaciones.todas NO es suya: es la cola de ms-notificaciones, que aun no
 * existe. Se declara aqui con su binding a turno.habilitado por la misma razon
 * que en la fase 5: sin una cola enlazada, el exchange descartaria el mensaje y
 * el publicador (mandatory) fallaria. Cuando ms-notificaciones la declare, tendra
 * que usar los mismos parametros (durable, sin argumentos).
 */
@Configuration
public class RabbitConfig {

	@Bean
	Queue colaEventoPublicado() {
		return QueueBuilder.durable(Mensajeria.COLA_EVENTO_PUBLICADO).build();
	}

	@Bean
	Binding bindingEventoPublicado(@Qualifier("colaEventoPublicado") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.EVENTO_PUBLICADO);
	}

	@Bean
	Queue colaNotificaciones() {
		return QueueBuilder.durable(Mensajeria.NOTIFICACIONES_TODAS).build();
	}

	@Bean
	Binding bindingTurnoHabilitado(@Qualifier("colaNotificaciones") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.TURNO_HABILITADO);
	}
}
