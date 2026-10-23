package com.teleticket.cola.entity;

/**
 * Si ya se hizo el sorteo de un evento.
 *
 * Solo avanza: PENDIENTE -> REALIZADO, una vez. Nunca vuelve a PENDIENTE,
 * y eso es lo que impide reordenar la cola despues de la hora anunciada.
 */
public enum EstadoSorteo {
	PENDIENTE,
	REALIZADO
}
