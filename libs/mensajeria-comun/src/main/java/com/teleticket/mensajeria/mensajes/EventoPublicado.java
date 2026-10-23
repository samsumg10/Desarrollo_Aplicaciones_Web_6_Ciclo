package com.teleticket.mensajeria.mensajes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Datos de evento.publicado (productor: ms-eventos).
 *
 * Lo consumen ms-cola (copia local de la hora de venta, para preparar la sala
 * de espera) y ms-inventario (crea el stock de cada zona).
 *
 * 'id' es el id del evento del catalogo. No confundir con SobreMensaje.eventoId,
 * que identifica al mensaje. Lleva todo lo que los consumidores necesitan para
 * no tener que llamar a ms-eventos en el momento de mas carga.
 */
public record EventoPublicado(
		UUID id,
		String nombre,
		Instant fecha,
		String recinto,
		Instant inicioVenta,
		Integer maxPorCliente,
		String reglasCola,
		List<ZonaPublicada> zonas) {
}
