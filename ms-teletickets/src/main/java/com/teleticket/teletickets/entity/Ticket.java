package com.teleticket.teletickets.entity;

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
@Table(name = "ticket")
@Getter
@NoArgsConstructor
public class Ticket {

	@Id
	private UUID id;

	@Column(name = "orden_id", nullable = false)
	private UUID ordenId;

	@Column(nullable = false)
	private Integer numero;

	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;

	@Column(name = "evento_id", nullable = false)
	private UUID eventoId;

	@Column(name = "zona_id", nullable = false)
	private UUID zonaId;

	@Column(nullable = false)
	private UUID codigo;

	@Column(nullable = false, length = 64)
	private String firma;

	@Column(name = "condiciones_ingreso", nullable = false, columnDefinition = "TEXT")
	private String condicionesIngreso;

	@Column(name = "dni_nominado", length = 12)
	private String dniNominado;

	@Column(name = "nombre_nominado", length = 150)
	private String nombreNominado;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private EstadoTicket estado;

	@Column(name = "emitido_en", nullable = false)
	private Instant emitidoEn;

	@Column(name = "usado_en")
	private Instant usadoEn;
}
