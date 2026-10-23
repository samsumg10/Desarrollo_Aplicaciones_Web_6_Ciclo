package com.teleticket.cola.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Lo que envia ms-ordenes para devolver un pase que consumio pero con el que no
 * pudo completar la compra.
 */
public record DevolverPaseRequest(

		@NotNull(message = "El id del usuario es obligatorio")
		UUID usuarioId) {
}
