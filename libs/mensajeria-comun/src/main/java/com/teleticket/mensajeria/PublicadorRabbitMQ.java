package com.teleticket.mensajeria;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Publica en RabbitMQ y espera la confirmacion del broker (publisher confirms).
 *
 * Sin confirmaciones, convertAndSend() devuelve en cuanto escribe en el socket:
 * si el broker no llego a guardar el mensaje, nadie se entera y el mensaje se
 * pierde. Con confirmaciones, el broker responde "lo recibi" (ack) o "no pude"
 * (nack), y aqui se reintenta si no hubo ack.
 *
 * Hay un caso mas: si el exchange existe pero ninguna cola esta enlazada a esa
 * routing key, el broker confirma igual y descarta el mensaje. Por eso tambien
 * se comprueba que no haya sido "devuelto" (publisher returns).
 *
 * Requiere en el servicio que publica:
 *   spring.rabbitmq.publisher-confirm-type=correlated
 *   spring.rabbitmq.publisher-returns=true
 *   spring.rabbitmq.template.mandatory=true
 */
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
			// Un CorrelationData nuevo por intento: su 'future' solo se completa una vez.
			CorrelationData correlacion = new CorrelationData(mensaje.eventoId().toString());
			try {
				plantilla.convertAndSend(Mensajeria.EXCHANGE, routingKey, mensaje, m -> {
					// message_id visible en la consola de RabbitMQ, sin abrir el JSON.
					m.getMessageProperties().setMessageId(mensaje.eventoId().toString());
					return m;
				}, correlacion);

				CorrelationData.Confirm confirmacion = correlacion.getFuture()
						.get(ESPERA_CONFIRMACION_SEGUNDOS, TimeUnit.SECONDS);

				if (!confirmacion.isAck()) {
					throw new IllegalStateException("el broker rechazo el mensaje (nack): " + confirmacion.reason());
				}
				if (correlacion.getReturned() != null) {
					// No se reintenta: sin cola enlazada no es un fallo pasajero.
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

	/** Sin confirmaciones activas el future nunca se completaria y cada publicacion esperaria 5 s en vano. */
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
