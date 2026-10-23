package com.teleticket.cola.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * El lugar de una persona en la sala de espera de un evento.
 *
 * Hay a lo sumo uno por persona y evento (UNIQUE en la base). Sus transiciones
 * de estado son metodos con nombre, no setters sueltos: asi no se puede, por
 * ejemplo, dar una posicion a un turno ya habilitado.
 */
@Entity
@Table(name = "turno")
@Getter
@NoArgsConstructor
public class Turno {

	@Id
	private UUID id;

	@Column(name = "evento_id", nullable = false)
	private UUID eventoId;

	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;

	/** Null mientras este EN_SALA. La asigna el sorteo, y desde entonces no cambia. */
	@Column
	private Integer posicion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private EstadoTurno estado;

	@Column(name = "ingreso_en", nullable = false)
	private Instant ingresoEn;

	@Column(name = "habilitado_en")
	private Instant habilitadoEn;

	/**
	 * Un turno nuevo. Con 'posicion' null nace EN_SALA (antes del sorteo); con
	 * posicion nace EN_COLA (llegada tardia, RN-02).
	 */
	public static Turno crear(UUID eventoId, UUID usuarioId, Integer posicion) {
		Turno t = new Turno();
		t.id = UUID.randomUUID();
		t.eventoId = eventoId;
		t.usuarioId = usuarioId;
		t.posicion = posicion;
		t.estado = posicion == null ? EstadoTurno.EN_SALA : EstadoTurno.EN_COLA;
		t.ingresoEn = Instant.now();
		return t;
	}

	/** Para quien quedo en la sala justo durante el sorteo: se ubica al final, por orden de llegada. */
	public void ubicarEnCola(int nuevaPosicion) {
		if (estado != EstadoTurno.EN_SALA) {
			throw new IllegalStateException("Solo un turno EN_SALA puede recibir posicion");
		}
		this.posicion = nuevaPosicion;
		this.estado = EstadoTurno.EN_COLA;
	}

	/** Le toco: pasa a HABILITADO. El pase lo crea quien llama. */
	public void habilitar() {
		if (estado != EstadoTurno.EN_COLA) {
			throw new IllegalStateException("Solo un turno EN_COLA puede habilitarse");
		}
		this.estado = EstadoTurno.HABILITADO;
		this.habilitadoEn = Instant.now();
	}
}
