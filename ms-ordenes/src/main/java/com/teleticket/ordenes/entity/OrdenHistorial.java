package com.teleticket.ordenes.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Una fila de auditoria: la orden paso de un estado a otro, por que y cuando.
 *
 * Solo se inserta. La primera fila de cada orden tiene estadoAnterior null.
 */
@Entity
@Table(name = "orden_historial")
@Getter
@NoArgsConstructor
public class OrdenHistorial {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "orden_id", nullable = false)
	private UUID ordenId;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_anterior", length = 20)
	private EstadoOrden estadoAnterior;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_nuevo", nullable = false, length = 20)
	private EstadoOrden estadoNuevo;

	@Column(nullable = false, length = 300)
	private String motivo;

	@Column(nullable = false)
	private Instant fecha;

	public static OrdenHistorial registrar(UUID ordenId, EstadoOrden anterior, EstadoOrden nuevo, String motivo) {
		OrdenHistorial h = new OrdenHistorial();
		h.ordenId = ordenId;
		h.estadoAnterior = anterior;
		h.estadoNuevo = nuevo;
		h.motivo = motivo;
		h.fecha = Instant.now();
		return h;
	}
}
