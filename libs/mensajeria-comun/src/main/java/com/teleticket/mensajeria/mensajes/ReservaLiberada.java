package com.teleticket.mensajeria.mensajes;

import java.util.UUID;

public record ReservaLiberada(
		UUID reservaId,
		UUID ordenId,
		UUID zonaId,
		Integer cantidad) {
}
