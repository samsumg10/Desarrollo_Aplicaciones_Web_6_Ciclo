package com.teleticket.mensajeria;

/**
 * Todos los nombres de RabbitMQ en un solo sitio.
 *
 * Ningun servicio escribe "evento.publicado" a mano: usa la constante. Asi un
 * nombre mal escrito es un error de compilacion y no un mensaje que se pierde
 * en silencio (el exchange acepta cualquier routing key, exista o no quien la
 * escuche).
 *
 * Convenciones (seccion 6.2 del documento de arquitectura):
 *   routing key:  <entidad>.<hecho en pasado>            orden.pagada
 *   cola:         <servicio-consumidor>.<routing-key>    tickets.orden-pagada
 */
public final class Mensajeria {

	private Mensajeria() {
	}

	// =================================================================
	// Exchanges
	// =================================================================

	/** Por aqui pasan todos los mensajes de negocio. Tipo topic. */
	public static final String EXCHANGE = "teleticket.exchange";

	/** Recibe los mensajes muertos (TTL vencido o rechazados). Tipo topic. */
	public static final String DLX = "teleticket.dlx";

	// =================================================================
	// Routing keys: la "direccion" escrita en el sobre
	// =================================================================

	public static final String EVENTO_PUBLICADO = "evento.publicado";
	public static final String TURNO_HABILITADO = "turno.habilitado";
	public static final String RESERVA_VENCIDA = "reserva.vencida";
	public static final String RESERVA_LIBERADA = "reserva.liberada";
	public static final String ORDEN_PAGADA = "orden.pagada";
	public static final String ORDEN_EXPIRADA = "orden.expirada";
	public static final String ORDEN_REEMBOLSADA = "orden.reembolsada";
	public static final String TICKETS_EMITIDOS = "tickets.emitidos";
	public static final String EMISION_FALLIDA = "emision.fallida";
	public static final String REEMBOLSO_SOLICITADO = "reembolso.solicitado";
	public static final String PAGO_REEMBOLSADO = "pago.reembolsado";
	public static final String TICKET_VALIDADO = "ticket.validado";

	// =================================================================
	// Colas: el "buzon" de cada consumidor
	//
	// Se listan todas las de la seccion 6.2 para que ninguna se escriba a
	// mano en las fases siguientes. Cada servicio declara solo las suyas.
	// =================================================================

	public static final String COLA_EVENTO_PUBLICADO = "cola.evento-publicado";
	public static final String INVENTARIO_EVENTO_PUBLICADO = "inventario.evento-publicado";
	public static final String INVENTARIO_RESERVA_ESPERA = "inventario.reserva-espera";
	public static final String INVENTARIO_RESERVA_VENCIDA = "inventario.reserva-vencida";
	public static final String INVENTARIO_ORDEN_PAGADA = "inventario.orden-pagada";
	public static final String INVENTARIO_ORDEN_REEMBOLSADA = "inventario.orden-reembolsada";
	public static final String TICKETS_ORDEN_PAGADA = "tickets.orden-pagada";
	public static final String TICKETS_ORDEN_REEMBOLSADA = "tickets.orden-reembolsada";

	/**
	 * Cola de mensajes muertos de tickets.orden-pagada (fase 11): recibe los orden.pagada que agotaron
	 * sus reintentos de emision. Tiene dos nombres para lo mismo: este es la cola y la routing key con
	 * que el DLX la alcanza.
	 */
	public static final String TICKETS_ORDEN_PAGADA_DLQ = "tickets.orden-pagada.dlq";
	public static final String ORDENES_TICKETS_EMITIDOS = "ordenes.tickets-emitidos";
	public static final String ORDENES_EMISION_FALLIDA = "ordenes.emision-fallida";
	public static final String ORDENES_PAGO_REEMBOLSADO = "ordenes.pago-reembolsado";
	public static final String ORDENES_RESERVA_LIBERADA = "ordenes.reserva-liberada";
	public static final String PAGOS_REEMBOLSO_SOLICITADO = "pagos.reembolso-solicitado";
	public static final String NOTIFICACIONES_TODAS = "notificaciones.todas";
}
