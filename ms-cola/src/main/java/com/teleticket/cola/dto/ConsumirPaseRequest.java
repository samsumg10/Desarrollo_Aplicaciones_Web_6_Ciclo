package com.teleticket.cola.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Lo que envia ms-ordenes para consumir un pase: la persona que lo presenta.
 * Si no coincide con la duenia del pase, el pase no se consume.
 */
public record ConsumirPaseRequest(

		@NotNull(message = "El id del usuario es obligatorio")
		UUID usuarioId) {
}
