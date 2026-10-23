package com.teleticket.cola.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * El turno tal como lo ve su duenio. Lo que trae depende del estado:
 *
 *   EN_SALA     posicion null; 'inicioVenta' dice cuando es el sorteo
 *   EN_COLA     posicion (la del sorteo, no cambia), cuantos van delante y espera estimada
 *   HABILITADO  el pase de compra
 *   EXPIRADO    el pase que vencio
 *
 * 'posicion' es la posicion del sorteo, fija y auditable. 'turnosDelante' es
 * cuantas personas siguen esperando por delante de ti ahora mismo (baja a medida
 * que se habilitan grupos), y de ahi sale la espera estimada.
 */
public record TurnoResponse(
		UUID turnoId,
		UUID eventoId,
		String estado,
		Integer posicion,
		Long turnosDelante,
		Long esperaEstimadaSegundos,
		Instant inicioVenta,
		PaseResponse pase) {
}
