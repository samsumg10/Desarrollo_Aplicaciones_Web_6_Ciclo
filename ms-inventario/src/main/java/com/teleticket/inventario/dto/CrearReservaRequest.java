package com.teleticket.inventario.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Lo que envia ms-ordenes para reservar entradas.
 *
 * El limite de entradas por comprador (RN-05) NO se valida aqui: lo conoce
 * ms-eventos y lo aplica ms-ordenes antes de llamar. Este servicio solo
 * responde por una cosa: si queda stock.
 */
public record CrearReservaRequest(

		@NotNull(message = "El id de la orden es obligatorio")
		UUID ordenId,

		@NotNull(message = "El id de la zona es obligatorio")
		UUID zonaId,

		@NotNull(message = "La cantidad es obligatoria")
		@Min(value = 1, message = "La cantidad minima es 1")
		Integer cantidad) {
}
