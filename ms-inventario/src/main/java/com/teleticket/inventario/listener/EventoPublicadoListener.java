package com.teleticket.inventario.listener;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;
import com.teleticket.inventario.exception.MensajeInvalidoException;
import com.teleticket.inventario.service.StockService;
import com.teleticket.mensajeria.Mensajeria;
import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.EventoPublicado;

/**
 * El otro lado del cartero: recibe evento.publicado y crea el stock.
 *
 * La confirmacion (ack) es MANUAL (ver application.properties). El orden es:
 *
 *   1. guardar en la base de datos      (StockService, con su transaccion)
 *   2. confirmar a RabbitMQ             (basicAck)
 *
 * Si el servicio se cae entre 1 y 2, RabbitMQ no recibio el ack y devuelve el
 * mensaje a la cola; al volver a llegar, mensaje_procesado lo reconoce y lo
 * descarta sin duplicar nada. Si se cayera ANTES de 1, el mensaje tampoco se
 * confirmo y se procesa completo. En ningun caso se pierde.
 */
@Component
public class EventoPublicadoListener {

	private static final Logger log = LoggerFactory.getLogger(EventoPublicadoListener.class);

	private final StockService stockService;

	public EventoPublicadoListener(StockService stockService) {
		this.stockService = stockService;
	}

	/**
	 * Spring convierte el JSON a SobreMensaje<EventoPublicado> mirando el tipo
	 * del primer parametro, y entrega el canal y el numero de entrega para
	 * poder confirmar a mano.
	 */
	@RabbitListener(queues = Mensajeria.INVENTARIO_EVENTO_PUBLICADO)
	public void alRecibir(SobreMensaje<EventoPublicado> sobre, Channel canal,
			@Header(AmqpHeaders.DELIVERY_TAG) long entrega) throws IOException {
		try {
			boolean nuevo = stockService.crearStockDeEvento(sobre);
			canal.basicAck(entrega, false);
			log.info("evento.publicado {} {}", sobre.eventoId(), nuevo ? "procesado" : "descartado (repetido)");

		} catch (MensajeInvalidoException e) {
			// Reintentarlo no lo arregla. Se descarta (requeue=false) para no entrar en un bucle.
			log.error("Mensaje invalido, se descarta: {}", e.getMessage());
			canal.basicReject(entrega, false);

		} catch (Exception e) {
			// Fallo pasajero (base de datos caida, por ejemplo): vuelve a la cola para reintentarlo.
			log.error("No se pudo procesar el mensaje, vuelve a la cola: {}", e.toString());
			canal.basicNack(entrega, false, true);
		}
	}
}
