package com.teleticket.pagos.pasarela;

public record ResultadoPasarela(boolean aprobado, String referencia, String motivo) {

	public static ResultadoPasarela aprobada(String referencia) {
		return new ResultadoPasarela(true, referencia, null);
	}

	public static ResultadoPasarela rechazada(String motivo) {
		return new ResultadoPasarela(false, null, motivo);
	}
}
