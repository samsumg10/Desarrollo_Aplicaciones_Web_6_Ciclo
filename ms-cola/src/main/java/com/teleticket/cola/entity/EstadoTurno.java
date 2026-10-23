package com.teleticket.cola.entity;

/**
 * Donde esta una persona en la sala de espera.
 *
 *   EN_SALA ----(sorteo)----> EN_COLA ----(ciclo de admision)----> HABILITADO
 *                                                                      |
 *                                                  (pase vencido sin usar)
 *                                                                      v
 *                                                                  EXPIRADO
 *
 * Quien entra despues del sorteo salta EN_SALA: nace EN_COLA con la ultima
 * posicion (RN-02).
 */
public enum EstadoTurno {

	/** Entro a la sala; el sorteo aun no ocurre, asi que no tiene posicion. */
	EN_SALA,

	/** Ya tiene posicion y espera su ciclo. */
	EN_COLA,

	/** Le toco: tiene un pase de compra. */
	HABILITADO,

	/** Su pase vencio sin usarse. */
	EXPIRADO
}
