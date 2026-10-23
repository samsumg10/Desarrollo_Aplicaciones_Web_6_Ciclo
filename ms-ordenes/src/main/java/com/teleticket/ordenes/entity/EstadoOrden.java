package com.teleticket.ordenes.entity;

public enum EstadoOrden {

	RESERVADA,

	PENDIENTE_PAGO,

	PAGADA,

	EMITIDA,

	EXPIRADA,

	REEMBOLSADA;

	public boolean puedePasarA(EstadoOrden hacia) {
		return switch (this) {
			case RESERVADA -> hacia == PENDIENTE_PAGO || hacia == EXPIRADA;
			case PENDIENTE_PAGO -> hacia == PAGADA || hacia == RESERVADA || hacia == EXPIRADA;
			case PAGADA -> hacia == EMITIDA || hacia == REEMBOLSADA;
			case EMITIDA -> hacia == REEMBOLSADA;
			case EXPIRADA, REEMBOLSADA -> false;
		};
	}
}
