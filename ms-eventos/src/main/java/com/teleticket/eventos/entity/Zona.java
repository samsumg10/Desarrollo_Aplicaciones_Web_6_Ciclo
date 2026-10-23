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

/**
 * Un sector del recinto, con su precio y su aforo. Por ejemplo: VIP, Platea,
 * General.
 *
 * El proyecto trabaja por zonas y no por asientos numerados: el mapa de
 * asientos queda explicitamente fuera del alcance (seccion 6.2.2 del informe).
 */
@Entity
@Table(name = "zona", uniqueConstraints = @UniqueConstraint(
		name = "uq_zona_nombre", columnNames = { "evento_id", "nombre" }))
@Getter
@Setter
@NoArgsConstructor
public class Zona {

	@Id
	private UUID id;

	/**
	 * El evento al que pertenece.
	 *
	 * LAZY significa que al cargar una zona no se trae el evento completo
	 * hasta que alguien lo pida. Evita consultas innecesarias.
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "evento_id", nullable = false)
	private Evento evento;

	@Column(nullable = false, length = 100)
	private String nombre;

	/**
	 * Precio por entrada.
	 *
	 * BigDecimal y no double: los decimales de coma flotante pierden precision
	 * y con dinero eso termina en centimos que no cuadran. En la base es
	 * NUMERIC(10,2).
	 */
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal precio;

	/**
	 * Capacidad total de la zona.
	 *
	 * Este numero NO baja al vender: es el total. Lo disponible lo lleva
	 * ms-inventario, que crea su stock a partir de este valor cuando recibe el
	 * mensaje evento.publicado (fase 6). Asi se cumple la Ley 32415, que
	 * prohibe emitir mas entradas que el aforo autorizado.
	 */
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
