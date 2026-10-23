package com.teleticket.cola.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record DevolverPaseRequest(

		@NotNull(message = "El id del usuario es obligatorio")
		UUID usuarioId) {
}
