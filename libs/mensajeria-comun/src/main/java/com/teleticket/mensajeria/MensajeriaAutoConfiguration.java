package com.teleticket.mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration(afterName = {
		"org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
		"org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration" })
@ConditionalOnClass(RabbitTemplate.class)
public class MensajeriaAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(MensajeriaAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean(MessageConverter.class)
	MessageConverter conversorJson(ObjectProvider<JsonMapper> jsonMapper) {
		return new JacksonJsonMessageConverter(jsonMapper.getIfAvailable(() -> JsonMapper.builder().build()));
	}

	@Bean
	@ConditionalOnMissingBean(name = "exchangePrincipal")
	TopicExchange exchangePrincipal() {
		return new TopicExchange(Mensajeria.EXCHANGE, true, false);
	}

	@Bean
	@ConditionalOnMissingBean(name = "exchangeMuertos")
	TopicExchange exchangeMuertos() {
		return new TopicExchange(Mensajeria.DLX, true, false);
	}

	@Bean
	@ConditionalOnBean(RabbitTemplate.class)
	@ConditionalOnMissingBean(PublicadorEventos.class)
	PublicadorEventos publicadorEventos(RabbitTemplate plantilla) {
		return new PublicadorRabbitMQ(plantilla);
	}

	@Bean
	ApplicationRunner declararTopologiaAlArrancar(ObjectProvider<RabbitAdmin> admin) {
		return args -> admin.ifAvailable(a -> {
			try {
				a.initialize();
			} catch (RuntimeException e) {
				log.warn("RabbitMQ no disponible al arrancar; se declarara al conectar: {}", e.toString());
			}
		});
	}
}
