package com.teleticket.ordenes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Los datos de la tarjeta con la que se paga una orden. La orden va en la ruta y la persona en
 * X-Usuario-Id, nunca en el cuerpo.
 *
 * El numero de tarjeta solo viaja hasta ms-pagos: aqui no se guarda ni se escribe en ningun log.
 */
public record PagarRequest(

		@NotBlank(message = "El numero de tarjeta es obligatorio")
		@Pattern(regexp = "\\d{13,19}", message = "El numero de tarjeta debe tener entre 13 y 19 digitos")
		String numeroTarjeta,

		@NotBlank(message = "El titular es obligatorio")
		@Size(max = 100, message = "El titular admite hasta 100 caracteres")
		String titular) {
}
