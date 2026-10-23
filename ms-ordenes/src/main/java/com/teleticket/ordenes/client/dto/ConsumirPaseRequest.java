package com.teleticket.ordenes.client.dto;

import java.util.UUID;

public record ConsumirPaseRequest(UUID usuarioId, UUID eventoId) {
}
