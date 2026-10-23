package com.teleticket.ordenes.client.dto;

import java.util.UUID;

public record CrearReservaRequest(UUID ordenId, UUID zonaId, Integer cantidad) {
}
