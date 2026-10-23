package com.teleticket.eventos.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evento")
@Getter
@Setter
@NoArgsConstructor
public class Evento {

	@Id
	private UUID id;

	@Column(nullable = false, length = 200)
	private String nombre;

	@Column(nullable = false)
	private Instant fecha;

	@Column(nullable = false, length = 200)
	private String recinto;

	@Column(name = "inicio_venta", nullable = false)
	private Instant inicioVenta;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoEvento estado;

	@Column(name = "max_por_cliente", nullable = false)
	private Integer maxPorCliente;

	@Column(name = "condiciones_ingreso", columnDefinition = "TEXT")
	private String condicionesIngreso;

	@Column(name = "reglas_cola", columnDefinition = "TEXT")
	private String reglasCola;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	@Column(name = "publicado_en")
	private Instant publicadoEn;

	@OneToMany(mappedBy = "evento", cascade = CascadeType.ALL,
			orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("precio DESC")
	private List<Zona> zonas = new ArrayList<>();

	public static Evento crear(String nombre, Instant fecha, String recinto,
			Instant inicioVenta, Integer maxPorCliente,
			String condicionesIngreso, String reglasCola) {
		Evento e = new Evento();
		e.id = UUID.randomUUID();
		e.nombre = nombre;
		e.fecha = fecha;
		e.recinto = recinto;
		e.inicioVenta = inicioVenta;
		e.maxPorCliente = maxPorCliente;
		e.condicionesIngreso = condicionesIngreso;
		e.reglasCola = reglasCola;
		e.estado = EstadoEvento.BORRADOR;
		e.creadoEn = Instant.now();
		return e;
	}

	public void agregarZona(Zona zona) {
		zona.setEvento(this);
		this.zonas.add(zona);
	}

	public boolean estaCongelado() {
		return estado != EstadoEvento.BORRADOR;
	}

	public int aforoTotal() {
		return zonas.stream().mapToInt(Zona::getAforo).sum();
	}
}
