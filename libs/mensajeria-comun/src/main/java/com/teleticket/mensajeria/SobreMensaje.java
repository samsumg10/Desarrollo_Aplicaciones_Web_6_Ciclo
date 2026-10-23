package com.teleticket.mensajeria;

import java.time.Instant;
import java.util.UUID;

/**
 * La envoltura estandar de todo mensaje, igual para los 11 tipos.
 *
 * Es el "sobre": lleva los datos de control, y dentro va la "carta" ({@code datos}).
 * Todos los servicios leen y escriben el mismo sobre, asi que cualquiera puede
 * rastrear o descartar un mensaje sin saber que contiene.
 *
 * Ejemplo en la consola de RabbitMQ:
 *
 *   {
 *     "eventoId": "3f2c...",              <- identifica ESTE mensaje, no el evento del catalogo
 *     "tipo": "evento.publicado",
 *     "fecha": "2026-10-23T03:35:45Z",
 *     "correlationId": "a8b308ed-...",    <- el X-Request-Id que puso el Gateway
 *     "datos": { ... }
 *   }
 *
 * OJO con el nombre: 'eventoId' es el del mensaje (asi lo llama el informe,
 * seccion 6.2) y no tiene relacion con un evento del catalogo de espectaculos.
 * Es la clave que los consumidores guardan en mensaje_procesado para descartar
 * repetidos.
 *
 * @param eventoId      UUID unico del mensaje; sirve para la idempotencia
 * @param tipo          routing key con la que se publico
 * @param fecha         cuando se creo el mensaje
 * @param correlationId el X-Request-Id de la peticion que lo origino; une los
 *                      logs de todos los servicios por los que paso
 * @param datos         el contenido, segun el tipo (ver el paquete mensajes)
 */
public record SobreMensaje<T>(
		UUID eventoId,
		String tipo,
		Instant fecha,
		String correlationId,
		T datos) {

	/**
	 * Crea el sobre con un id nuevo y la hora actual.
	 *
	 * Si no hay correlationId (por ejemplo, un mensaje que nace de una tarea
	 * programada y no de una peticion), se genera uno para no dejar el campo vacio.
	 */
	public static <T> SobreMensaje<T> de(String tipo, String correlationId, T datos) {
		return new SobreMensaje<>(
				UUID.randomUUID(),
				tipo,
				Instant.now(),
				correlationId != null && !correlationId.isBlank() ? correlationId : UUID.randomUUID().toString(),
				datos);
	}
}
