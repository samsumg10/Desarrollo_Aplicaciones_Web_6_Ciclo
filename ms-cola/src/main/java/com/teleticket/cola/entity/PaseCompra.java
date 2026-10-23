package com.teleticket.cola.entity;

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
@Table(name = "pase_compra")
@Getter
@NoArgsConstructor
public class PaseCompra {

	@Id
	private UUID codigo;

	@Column(name = "turno_id", nullable = false)
	private UUID turnoId;

	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;

	@Column(name = "evento_id", nullable = false)
	private UUID eventoId;

	@Column(name = "expira_en", nullable = false)
	private Instant expiraEn;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private EstadoPase estado;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	public static PaseCompra crear(Turno turno, Duration vigencia) {
		PaseCompra p = new PaseCompra();
		p.codigo = UUID.randomUUID();
		p.turnoId = turno.getId();
		p.usuarioId = turno.getUsuarioId();
		p.eventoId = turno.getEventoId();
		p.estado = EstadoPase.VIGENTE;
		p.creadoEn = Instant.now();
		p.expiraEn = p.creadoEn.plus(vigencia);
		return p;
	}
}
