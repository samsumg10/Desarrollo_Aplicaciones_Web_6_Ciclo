package com.teleticket.ordenes.client.dto;

import java.util.UUID;

/** Cuerpo de POST /internal/reservas de ms-inventario. */
public record CrearReservaRequest(UUID ordenId, UUID zonaId, Integer cantidad) {
}
