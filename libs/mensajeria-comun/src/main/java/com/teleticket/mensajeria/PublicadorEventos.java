package com.teleticket.mensajeria;

/**
 * La unica puerta por la que un servicio publica un mensaje.
 *
 * Los servicios dependen de esta interfaz y nunca de RabbitTemplate. La
 * seccion 11 del diseno preve que algunos hechos (evento.publicado,
 * orden.pagada, tickets.emitidos, ticket.validado) migren a Kafka: cuando
 * llegue ese dia se escribe otra implementacion de esta interfaz y el codigo
 * de negocio no cambia.
 */
public interface PublicadorEventos {

	/**
	 * Publica el mensaje y vuelve solo cuando el broker confirma que lo recibio.
	 *
	 * @param routingKey la direccion; usar las constantes de {@link Mensajeria}
	 * @param mensaje    el sobre ya armado con {@link SobreMensaje#de}
	 * @throws PublicacionFallidaException si el broker no confirma tras los reintentos
	 */
	void publicar(String routingKey, SobreMensaje<?> mensaje);
}
