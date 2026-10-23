package com.teleticket.inventario.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Cuantas entradas quedan en una zona.
 *
 * Solo tiene getters, y es a proposito: el stock NUNCA se modifica con
 * setDisponible() + save(). Eso seria "leer, restar en Java, escribir", y dos
 * peticiones simultaneas leerian el mismo valor y la segunda pisaria a la
 * primera (sobreventa). El stock cambia unicamente con las consultas atomicas
 * de StockZonaRepository, que restan dentro de la propia base de datos.
 *
 * La fila la crea el consumidor de evento.publicado, con disponible = total.
 */
@Entity
@Table(name = "stock_zona")
@Getter
@NoArgsConstructor
public class StockZona {

	/** Es el mismo id que la zona tiene en ms-eventos. */
	@Id
	@Column(name = "zona_id")
	private UUID zonaId;

	@Column(name = "evento_id", nullable = false)
	private UUID eventoId;

	/** El aforo de la zona. No baja al vender. */
	@Column(nullable = false)
	private Integer total;

	@Column(nullable = false)
	private Integer disponible;

	/** Cuantas veces cambio el stock. Solo informativa: ver schema.sql. */
	@Column(nullable = false)
	private Long version;
}
