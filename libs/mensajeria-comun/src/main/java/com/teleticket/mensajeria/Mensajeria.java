package com.teleticket.mensajeria;

public final class Mensajeria {

	private Mensajeria() {
	}

	public static final String EXCHANGE = "teleticket.exchange";

	public static final String DLX = "teleticket.dlx";

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

	public static final String COLA_EVENTO_PUBLICADO = "cola.evento-publicado";
	public static final String INVENTARIO_EVENTO_PUBLICADO = "inventario.evento-publicado";
	public static final String INVENTARIO_RESERVA_ESPERA = "inventario.reserva-espera";
	public static final String INVENTARIO_RESERVA_VENCIDA = "inventario.reserva-vencida";
	public static final String INVENTARIO_ORDEN_PAGADA = "inventario.orden-pagada";
	public static final String INVENTARIO_ORDEN_REEMBOLSADA = "inventario.orden-reembolsada";
	public static final String TICKETS_ORDEN_PAGADA = "tickets.orden-pagada";
	public static final String TICKETS_ORDEN_REEMBOLSADA = "tickets.orden-reembolsada";

	public static final String TICKETS_ORDEN_PAGADA_DLQ = "tickets.orden-pagada.dlq";
	public static final String ORDENES_TICKETS_EMITIDOS = "ordenes.tickets-emitidos";
	public static final String ORDENES_EMISION_FALLIDA = "ordenes.emision-fallida";
	public static final String ORDENES_PAGO_REEMBOLSADO = "ordenes.pago-reembolsado";
	public static final String ORDENES_RESERVA_LIBERADA = "ordenes.reserva-liberada";
	public static final String PAGOS_REEMBOLSO_SOLICITADO = "pagos.reembolso-solicitado";
	public static final String NOTIFICACIONES_TODAS = "notificaciones.todas";
}
