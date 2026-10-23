package com.teleticket.mensajeria;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class PublicadorRabbitMQ implements PublicadorEventos {

	private static final Logger log = LoggerFactory.getLogger(PublicadorRabbitMQ.class);

	private static final int INTENTOS = 3;
	private static final long ESPERA_CONFIRMACION_SEGUNDOS = 5;
	private static final long PAUSA_ENTRE_INTENTOS_MS = 300;

	private final RabbitTemplate plantilla;

	public PublicadorRabbitMQ(RabbitTemplate plantilla) {
		this.plantilla = plantilla;
	}

	@Override
	public void publicar(String routingKey, SobreMensaje<?> mensaje) {
		comprobarConfiguracion();

		Exception ultimoError = null;
		for (int intento = 1; intento <= INTENTOS; intento++) {
			CorrelationData correlacion = new CorrelationData(mensaje.eventoId().toString());
			try {
				plantilla.convertAndSend(Mensajeria.EXCHANGE, routingKey, mensaje, m -> {
					m.getMessageProperties().setMessageId(mensaje.eventoId().toString());
					return m;
				}, correlacion);

				CorrelationData.Confirm confirmacion = correlacion.getFuture()
						.get(ESPERA_CONFIRMACION_SEGUNDOS, TimeUnit.SECONDS);

				if (!confirmacion.isAck()) {
					throw new IllegalStateException("el broker rechazo el mensaje (nack): " + confirmacion.reason());
				}
				if (correlacion.getReturned() != null) {
					throw new PublicacionFallidaException(
							"Ninguna cola recibio '%s': falta declarar el binding".formatted(routingKey), null);
				}

				log.info("Mensaje {} publicado ({})", routingKey, mensaje.eventoId());
				return;

			} catch (PublicacionFallidaException e) {
				throw e;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new PublicacionFallidaException("Publicacion interrumpida", e);
			} catch (ExecutionException | TimeoutException | RuntimeException e) {
				ultimoError = e;
				log.warn("Intento {}/{} de publicar '{}' fallo: {}", intento, INTENTOS, routingKey, e.toString());
				pausar();
			}
		}
		throw new PublicacionFallidaException(
				"No se pudo publicar '%s' tras %d intentos".formatted(routingKey, INTENTOS), ultimoError);
	}

	private void comprobarConfiguracion() {
		if (!plantilla.getConnectionFactory().isPublisherConfirms()) {
			throw new IllegalStateException(
					"Faltan los publisher confirms: agrega spring.rabbitmq.publisher-confirm-type=correlated");
		}
	}

	private void pausar() {
		try {
			Thread.sleep(PAUSA_ENTRE_INTENTOS_MS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
