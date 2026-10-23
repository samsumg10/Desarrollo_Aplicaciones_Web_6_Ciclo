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

	@Column
	private Integer posicion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private EstadoTurno estado;

	@Column(name = "ingreso_en", nullable = false)
	private Instant ingresoEn;

	@Column(name = "habilitado_en")
	private Instant habilitadoEn;

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

	public void ubicarEnCola(int nuevaPosicion) {
		if (estado != EstadoTurno.EN_SALA) {
			throw new IllegalStateException("Solo un turno EN_SALA puede recibir posicion");
		}
		this.posicion = nuevaPosicion;
		this.estado = EstadoTurno.EN_COLA;
	}

	public void habilitar() {
		if (estado != EstadoTurno.EN_COLA) {
			throw new IllegalStateException("Solo un turno EN_COLA puede habilitarse");
		}
		this.estado = EstadoTurno.HABILITADO;
		this.habilitadoEn = Instant.now();
	}
}
