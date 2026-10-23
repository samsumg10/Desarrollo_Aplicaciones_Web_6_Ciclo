package com.teleticket.ordenes.client.dto;

import java.util.UUID;

/** Cuerpo de POST /internal/pases/{codigo}/devolver de ms-cola. */
public record DevolverPaseRequest(UUID usuarioId) {
}
