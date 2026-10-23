package com.teleticket.ordenes.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CrearOrdenRequest(

		@NotNull(message = "El id del evento es obligatorio")
		UUID eventoId,

		@NotNull(message = "El id de la zona es obligatorio")
		UUID zonaId,

		@NotNull(message = "La cantidad es obligatoria")
		@Min(value = 1, message = "La cantidad minima es 1")
		Integer cantidad) {
}
