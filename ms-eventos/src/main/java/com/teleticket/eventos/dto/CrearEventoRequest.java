package com.teleticket.eventos.dto;

import java.time.Instant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearEventoRequest(

		@NotBlank(message = "El nombre del evento es obligatorio")
		@Size(max = 200, message = "El nombre no puede pasar de 200 caracteres")
		String nombre,

		@NotNull(message = "La fecha del evento es obligatoria")
		Instant fecha,

		@NotBlank(message = "El recinto es obligatorio")
		@Size(max = 200, message = "El recinto no puede pasar de 200 caracteres")
		String recinto,

		@NotNull(message = "La hora de inicio de venta es obligatoria")
		Instant inicioVenta,

		@NotNull(message = "El maximo por cliente es obligatorio")
		@Min(value = 1, message = "El maximo por cliente debe ser al menos 1")
		@Max(value = 20, message = "El maximo por cliente no puede pasar de 20")
		Integer maxPorCliente,

		@NotBlank(message = "Las condiciones de ingreso son obligatorias: lo informado es lo que se valida en la puerta")
		String condicionesIngreso,

		String reglasCola) {
}
