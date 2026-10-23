package com.teleticket.eventos.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Lo que ms-tickets copia en cada e-ticket (fase 10).
 *
 * Lo pide por Feign a GET /internal/eventos/{id}/condiciones.
 *
 * Esta es la pieza tecnica que resuelve PRB-06. En 2022 Teleticket fue multada
 * con 2,57 UIT porque anuncio una entrada valida en formato digital y luego
 * exigio imprimirla. Aqui el texto de las condiciones viaja desde ms-eventos
 * hasta la entrada y hasta el correo, asi que lo informado en la compra es
 * literalmente lo mismo que se valida en la puerta.
 */
public record CondicionesResponse(
		UUID eventoId,
		String nombreEvento,
		Instant fechaEvento,
		String recinto,
		String condicionesIngreso) {
}
