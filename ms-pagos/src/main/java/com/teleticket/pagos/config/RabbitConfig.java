package com.teleticket.pagos.config;

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
	Queue colaPagosReembolsoSolicitado() {
		return QueueBuilder.durable(Mensajeria.PAGOS_REEMBOLSO_SOLICITADO).build();
	}

	@Bean
	Binding bindingPagosReembolsoSolicitado(@Qualifier("colaPagosReembolsoSolicitado") Queue cola,
			@Qualifier("exchangePrincipal") TopicExchange exchange) {
		return BindingBuilder.bind(cola).to(exchange).with(Mensajeria.REEMBOLSO_SOLICITADO);
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
}
