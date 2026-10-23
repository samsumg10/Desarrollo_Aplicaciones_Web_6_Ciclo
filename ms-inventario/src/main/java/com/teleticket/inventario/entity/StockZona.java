package com.teleticket.inventario.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock_zona")
@Getter
@NoArgsConstructor
public class StockZona {

	@Id
	@Column(name = "zona_id")
	private UUID zonaId;

	@Column(name = "evento_id", nullable = false)
	private UUID eventoId;

	@Column(nullable = false)
	private Integer total;

	@Column(nullable = false)
	private Integer disponible;

	@Column(nullable = false)
	private Long version;
}
