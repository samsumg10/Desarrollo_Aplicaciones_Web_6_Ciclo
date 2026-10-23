package com.teleticket.inventario.entity;

import java.time.Duration;
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
@Table(name = "reserva")
@Getter
@NoArgsConstructor
public class Reserva {

	public static final Duration DURACION = Duration.ofMinutes(10);

	@Id
	private UUID id;

	@Column(name = "orden_id", nullable = false)
	private UUID ordenId;

	@Column(name = "zona_id", nullable = false)
	private UUID zonaId;

	@Column(nullable = false)
	private Integer cantidad;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoReserva estado;

	@Column(name = "vence_en", nullable = false)
	private Instant venceEn;

	@Column(name = "creada_en", nullable = false)
	private Instant creadaEn;

	public static Reserva crear(UUID ordenId, UUID zonaId, Integer cantidad) {
		Reserva r = new Reserva();
		r.id = UUID.randomUUID();
		r.ordenId = ordenId;
		r.zonaId = zonaId;
		r.cantidad = cantidad;
		r.estado = EstadoReserva.ACTIVA;
		r.creadaEn = Instant.now();
		r.venceEn = r.creadaEn.plus(DURACION);
		return r;
	}

	public boolean haVencido() {
		return !Instant.now().isBefore(venceEn);
	}
}
