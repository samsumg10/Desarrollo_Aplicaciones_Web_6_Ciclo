package com.teleticket.pagos.entity;

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
@Table(name = "pago")
@Getter
@NoArgsConstructor
public class Pago {

	@Id
	private UUID id;

	@Column(name = "orden_id", nullable = false)
	private UUID ordenId;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal monto;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoPago estado;

	@Column(name = "referencia_pasarela", length = 40)
	private String referenciaPasarela;

	@Column(name = "motivo_rechazo", length = 200)
	private String motivoRechazo;

	@Column(name = "tarjeta_ultimos4", nullable = false, length = 4)
	private String tarjetaUltimos4;

	@Column(name = "idempotency_key", nullable = false, length = 64)
	private String idempotencyKey;

	@Column(nullable = false)
	private Integer intentos;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;
}
