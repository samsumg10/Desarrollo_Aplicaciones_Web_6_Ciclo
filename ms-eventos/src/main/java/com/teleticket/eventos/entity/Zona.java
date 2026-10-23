package com.teleticket.eventos.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "zona", uniqueConstraints = @UniqueConstraint(
		name = "uq_zona_nombre", columnNames = { "evento_id", "nombre" }))
@Getter
@Setter
@NoArgsConstructor
public class Zona {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "evento_id", nullable = false)
	private Evento evento;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal precio;

	@Column(nullable = false)
	private Integer aforo;

	public static Zona crear(String nombre, BigDecimal precio, Integer aforo) {
		Zona z = new Zona();
		z.id = UUID.randomUUID();
		z.nombre = nombre;
		z.precio = precio;
		z.aforo = aforo;
		return z;
	}
}
