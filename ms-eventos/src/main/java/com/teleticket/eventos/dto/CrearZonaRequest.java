package com.teleticket.eventos.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearZonaRequest(

		@NotBlank(message = "El nombre de la zona es obligatorio")
		@Size(max = 100, message = "El nombre no puede pasar de 100 caracteres")
		String nombre,

		@NotNull(message = "El precio es obligatorio")
		@DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
		@Digits(integer = 8, fraction = 2, message = "El precio admite como maximo 2 decimales")
		BigDecimal precio,

		@NotNull(message = "El aforo es obligatorio")
		@Min(value = 1, message = "El aforo debe ser al menos 1")
		Integer aforo) {
}
