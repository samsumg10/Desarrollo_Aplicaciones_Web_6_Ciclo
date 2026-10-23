package com.teleticket.cola.entity;

/**
 * Ciclo de vida de un pase de compra. Un pase se usa una sola vez (PRB-03).
 *
 *   VIGENTE --> USADO     lo consumio ms-ordenes al crear la orden
 *   VIGENTE --> VENCIDO   paso su hora de expiracion sin usarse
 */
public enum EstadoPase {
	VIGENTE,
	USADO,
	VENCIDO
}
