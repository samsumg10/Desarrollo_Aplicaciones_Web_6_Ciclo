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

/**
 * Las colas de ms-pagos (fase 11): la suya (pagos.reembolso-solicitado) y la de su consumidor
 * (ordenes.pago-reembolsado).
 *
 * Cada una se declara tambien en ms-ordenes, con los mismos parametros (QueueBuilder.durable sin argumentos).
 * Asi el mensaje tiene donde esperar aunque el servicio que lo va a leer este apagado: con
 * template.mandatory=true, un mensaje que no llega a ninguna cola hace fallar la publicacion.
 */
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

	/** Donde esperan los pago.reembolsado que ms-pagos publica y ms-ordenes consume. */
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
