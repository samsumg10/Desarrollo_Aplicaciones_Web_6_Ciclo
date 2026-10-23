package com.teleticket.cola.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Lo que envia ms-ordenes para consumir un pase: la persona que lo presenta y,
 * opcionalmente, el evento que quiere comprar.
 *
 * Si el usuario no coincide con la duenia del pase, el pase no se consume. Si se
 * indica 'eventoId' y no coincide con el evento del pase, tampoco: un pase de un
 * evento no sirve para comprar otro, y se comprueba en el mismo UPDATE que lo
 * consume (si se comprobara despues, el pase ya estaria gastado).
 */
public record ConsumirPaseRequest(

		@NotNull(message = "El id del usuario es obligatorio")
		UUID usuarioId,

		UUID eventoId) {
}
