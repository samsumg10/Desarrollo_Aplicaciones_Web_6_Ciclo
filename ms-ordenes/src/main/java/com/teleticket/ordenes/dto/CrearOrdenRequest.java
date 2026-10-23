package com.teleticket.ordenes.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Lo que envia el comprador para crear una orden. El pase va en la cabecera
 * X-Pase-Compra y la persona en X-Usuario-Id, nunca en el cuerpo: asi no se puede
 * comprar a nombre de otro con un JSON armado a mano.
 *
 * El maximo de entradas (RN-05) no se valida aqui: lo conoce ms-eventos.
 */
public record CrearOrdenRequest(

		@NotNull(message = "El id del evento es obligatorio")
		UUID eventoId,

		@NotNull(message = "El id de la zona es obligatorio")
		UUID zonaId,

		@NotNull(message = "La cantidad es obligatoria")
		@Min(value = 1, message = "La cantidad minima es 1")
		Integer cantidad) {
}
