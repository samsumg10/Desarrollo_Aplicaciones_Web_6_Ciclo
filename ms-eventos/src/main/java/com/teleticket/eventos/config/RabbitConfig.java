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

/**
 * Las dos colas que reciben evento.publicado, y su enlace con el exchange.
 *
 * Lo normal seria que cada consumidor declare su propia cola (ms-inventario la
 * suya, ms-cola la suya). Se declaran tambien aqui porque, hasta las fases 6 y
 * 7, nadie las ha creado: sin ellas el exchange descartaria el mensaje y no
 * habria nada que ver en la consola de RabbitMQ.
 *
 * Declarar dos veces la misma cola no da error mientras los parametros sean
 * identicos (por eso los consumidores usaran las mismas constantes y el mismo
 * QueueBuilder.durable sin argumentos extra).
 *
 * El exchange no se declara aqui: lo declara la libreria mensajeria-comun.
 */
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

	/**
	 * El binding es la regla "lo dirigido a evento.publicado va a este buzon".
	 * Con dos bindings a la misma routing key, un solo mensaje se copia en las
	 * dos colas: eso es publish/subscribe.
	 */
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
