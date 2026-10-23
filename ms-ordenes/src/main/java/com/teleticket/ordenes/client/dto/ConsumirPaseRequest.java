package com.teleticket.ordenes.client.dto;

import java.util.UUID;

/** Cuerpo de POST /internal/pases/{codigo}/consumir de ms-cola. */
public record ConsumirPaseRequest(UUID usuarioId, UUID eventoId) {
}
