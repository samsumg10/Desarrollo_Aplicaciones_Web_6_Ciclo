package com.teleticket.pagos.pasarela;

/**
 * Lo que responde la pasarela de pagos.
 *
 * @param aprobado   si cobro
 * @param referencia el identificador del cobro en la pasarela (solo si aprobo)
 * @param motivo     por que lo rechazo (solo si rechazo)
 */
public record ResultadoPasarela(boolean aprobado, String referencia, String motivo) {

	public static ResultadoPasarela aprobada(String referencia) {
		return new ResultadoPasarela(true, referencia, null);
	}

	public static ResultadoPasarela rechazada(String motivo) {
		return new ResultadoPasarela(false, null, motivo);
	}
}
