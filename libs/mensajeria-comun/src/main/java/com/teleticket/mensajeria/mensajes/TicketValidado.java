package com.teleticket.mensajeria.mensajes;

import java.time.Instant;
import java.util.UUID;

public record TicketValidado(
		UUID ticketId,
		UUID ordenId,
		UUID eventoId,
		UUID controladorId,
		Instant validadoEn) {
}
