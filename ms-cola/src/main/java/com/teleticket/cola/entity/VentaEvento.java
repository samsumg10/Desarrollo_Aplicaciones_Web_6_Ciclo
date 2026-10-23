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
@Table(name = "venta_evento")
@Getter
@NoArgsConstructor
public class VentaEvento {

	@Id
	@Column(name = "evento_id")
	private UUID eventoId;

	@Column(nullable = false, length = 200)
	private String nombre;

	@Column(name = "inicio_venta", nullable = false)
	private Instant inicioVenta;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_sorteo", nullable = false, length = 12)
	private EstadoSorteo estadoSorteo;

	@Column(name = "capacidad_por_ciclo", nullable = false)
	private Integer capacidadPorCiclo;

	@Column(name = "ultima_posicion", nullable = false)
	private Integer ultimaPosicion;

	@Column(name = "sorteo_en")
	private Instant sorteoEn;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	public int siguientePosicion() {
		ultimaPosicion = ultimaPosicion + 1;
		return ultimaPosicion;
	}
}
