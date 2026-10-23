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

/**
 * Se activa sola en cualquier servicio que tenga esta libreria en el classpath
 * (ver META-INF/spring/...AutoConfiguration.imports). Deja listo:
 *
 *   - los dos exchanges (teleticket.exchange y teleticket.dlx),
 *   - el conversor a JSON para publicar y para @RabbitListener,
 *   - el PublicadorEventos,
 *   - la declaracion de exchanges y colas al arrancar.
 *
 * Las COLAS no se declaran aqui: cada servicio declara las suyas.
 */
@AutoConfiguration(afterName = {
		"org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
		"org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration" })
@ConditionalOnClass(RabbitTemplate.class)
public class MensajeriaAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(MensajeriaAutoConfiguration.class);

	/**
	 * Los mensajes viajan como JSON y no como objetos Java serializados: asi se
	 * pueden leer en la consola de RabbitMQ y cualquier servicio, en cualquier
	 * lenguaje, puede consumirlos.
	 *
	 * Spring Boot lo engancha solo al RabbitTemplate y a los @RabbitListener.
	 * Se reutiliza el JsonMapper de Boot para que las fechas salgan igual que en
	 * las respuestas REST (ISO-8601 UTC).
	 */
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

	/**
	 * Declara exchanges y colas al arrancar, y no al primer mensaje.
	 *
	 * RabbitAdmin declara cuando se abre la primera conexion, y un servicio que
	 * solo publica no la abre hasta su primer envio. Sin esto, las colas
	 * no aparecerian en la consola hasta publicar algo.
	 *
	 * Si RabbitMQ esta caido no se aborta el arranque: el servicio debe seguir
	 * atendiendo lo que no necesite al broker, y RabbitAdmin volvera a declarar
	 * por su cuenta cuando la conexion se restablezca.
	 */
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
