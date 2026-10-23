package com.teleticket.ordenes.entity;

import java.math.BigDecimal;
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
@Table(name = "orden")
@Getter
@NoArgsConstructor
public class Orden {

	@Id
	private UUID id;

	@Column(nullable = false, length = 20)
	private String codigo;

	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;

	@Column(name = "evento_id", nullable = false)
	private UUID eventoId;

	@Column(name = "zona_id", nullable = false)
	private UUID zonaId;

	@Column(nullable = false)
	private Integer cantidad;

	@Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
	private BigDecimal precioUnitario;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal total;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoOrden estado;

	@Column(name = "pase_codigo", nullable = false)
	private UUID paseCodigo;

	@Column(name = "reserva_id", nullable = false)
	private UUID reservaId;

	@Column(name = "vence_en", nullable = false)
	private Instant venceEn;

	@Column(name = "creada_en", nullable = false)
	private Instant creadaEn;

	public static Orden crear(UUID id, UUID usuarioId, UUID eventoId, UUID zonaId, int cantidad,
			BigDecimal precioUnitario, UUID paseCodigo, UUID reservaId, Instant venceEn) {
		Orden o = new Orden();
		o.id = id;
		o.codigo = "OR-" + id.toString().replace("-", "").substring(0, 8).toUpperCase();
		o.usuarioId = usuarioId;
		o.eventoId = eventoId;
		o.zonaId = zonaId;
		o.cantidad = cantidad;
		o.precioUnitario = precioUnitario;
		o.total = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
		o.estado = EstadoOrden.RESERVADA;
		o.paseCodigo = paseCodigo;
		o.reservaId = reservaId;
		o.venceEn = venceEn;
		o.creadaEn = Instant.now();
		return o;
	}
}
