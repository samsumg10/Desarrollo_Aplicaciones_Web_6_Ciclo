package com.teleticket.pagos.pasarela;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * La pasarela de pagos de mentira (seccion 12 del documento de arquitectura).
 *
 * Decide por los ULTIMOS 4 digitos de la tarjeta, para poder probar cada caso a voluntad:
 *
 *   0000             rechazada
 *   9999             aprobada, pero tarda 5 segundos (para probar el timeout de quien llama)
 *   cualquier otra   aprobada
 *
 * Es el unico sitio que habria que cambiar para usar una pasarela real: el resto de
 * ms-pagos solo ve un ResultadoPasarela.
 */
@Component
public class PasarelaSimulada {

	private static final Logger log = LoggerFactory.getLogger(PasarelaSimulada.class);

	private static final long DEMORA_MS = 5_000;

	public ResultadoPasarela cobrar(String numeroTarjeta, BigDecimal monto) {
		String ultimos4 = numeroTarjeta.substring(numeroTarjeta.length() - 4);

		if ("0000".equals(ultimos4)) {
			log.info("Pasarela: tarjeta ****{} rechazada", ultimos4);
			return ResultadoPasarela.rechazada("Tarjeta rechazada por el banco emisor");
		}
		if ("9999".equals(ultimos4)) {
			log.info("Pasarela: tarjeta ****{} lenta, responde en {} ms", ultimos4, DEMORA_MS);
			dormir();
		}

		String referencia = "SIM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
		log.info("Pasarela: cobro de {} a la tarjeta ****{} aprobado ({})", monto, ultimos4, referencia);
		return ResultadoPasarela.aprobada(referencia);
	}

	private void dormir() {
		try {
			Thread.sleep(DEMORA_MS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Cobro interrumpido", e);
		}
	}
}
