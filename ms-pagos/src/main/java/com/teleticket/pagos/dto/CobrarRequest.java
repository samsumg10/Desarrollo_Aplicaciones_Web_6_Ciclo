package com.teleticket.pagos.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CobrarRequest(

		@NotNull(message = "El id de la orden es obligatorio")
		UUID ordenId,

		@NotNull(message = "El monto es obligatorio")
		@DecimalMin(value = "0.00", message = "El monto no puede ser negativo")
		BigDecimal monto,

		@NotBlank(message = "El numero de tarjeta es obligatorio")
		@Pattern(regexp = "\\d{13,19}", message = "El numero de tarjeta debe tener entre 13 y 19 digitos")
		String numeroTarjeta,

		@NotBlank(message = "El titular es obligatorio")
		@Size(max = 100, message = "El titular admite hasta 100 caracteres")
		String titular) {
}
