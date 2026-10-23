package com.teleticket.ordenes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PagarRequest(

		@NotBlank(message = "El numero de tarjeta es obligatorio")
		@Pattern(regexp = "\\d{13,19}", message = "El numero de tarjeta debe tener entre 13 y 19 digitos")
		String numeroTarjeta,

		@NotBlank(message = "El titular es obligatorio")
		@Size(max = 100, message = "El titular admite hasta 100 caracteres")
		String titular) {
}
