package com.teleticket.ordenes.config;

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
 * Las colas de orden.pagada que declara ms-ordenes, el productor.
 *
 * Por que las declara el productor y no solo el consumidor: con template.mandatory=true, un mensaje
 * que no llega a ninguna cola se devuelve y la publicacion falla. Si ms-inventario no hubiera
 * arrancado nunca, la cola no existiria y no se podria cobrar. Es el mismo criterio que usa
 * ms-eventos con evento.publicado.
 *
 * Declarar la misma cola dos veces no da error mientras los parametros sean identicos (ms-inventario
 * la declara igual: QueueBuilder.durable sin argumentos). Si algun dia se le agrega un argumento
 * (un DLX, por ejemplo), hay que cambiarlo en los dos sitios a la vez: RabbitMQ rechaza redeclarar
 * una cola con parametros distintos (PRECONDITION_FAILED).
 *
 * La cola de ms-tickets (tickets.orden-pagada) la declarara la fase 10.
 */
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
}
