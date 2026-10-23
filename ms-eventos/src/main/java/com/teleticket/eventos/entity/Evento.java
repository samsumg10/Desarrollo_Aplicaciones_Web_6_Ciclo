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

/**
 * Un evento del catalogo: concierto, partido, teatro.
 *
 * Una "entidad" es una clase Java que representa una fila de una tabla.
 * Spring Data JPA se encarga de traducir entre las dos, asi que el codigo
 * trabaja con objetos en vez de con SQL.
 *
 * Casos de uso: CU-02 (consultar), CU-14 (configurar), CU-15 (publicar).
 */
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

	/** Cuando ocurre el evento. */
	@Column(nullable = false)
	private Instant fecha;

	@Column(nullable = false, length = 200)
	private String recinto;

	/**
	 * Hora oficial de apertura de la venta. Se publica al comprador y, una vez
	 * el evento esta EN_VENTA, no se puede cambiar (RN-01).
	 */
	@Column(name = "inicio_venta", nullable = false)
	private Instant inicioVenta;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoEvento estado;

	/** Maximo de entradas por comprador y por evento (RN-05). Lo valida ms-ordenes. */
	@Column(name = "max_por_cliente", nullable = false)
	private Integer maxPorCliente;

	/**
	 * Que se exige para entrar el dia del evento (DNI, formato digital...).
	 *
	 * ms-tickets copia este texto en cada e-ticket y ms-notificaciones en cada
	 * correo, de modo que lo informado en la compra sea exactamente lo que se
	 * valida en la puerta. Es la respuesta a PRB-06: en 2022 Teleticket fue
	 * multada porque anuncio una entrada digital y luego la exigio impresa.
	 */
	@Column(name = "condiciones_ingreso", columnDefinition = "TEXT")
	private String condicionesIngreso;

	/** Reglas de la cola virtual, publicadas y congeladas al publicar (PRB-02). */
	@Column(name = "reglas_cola", columnDefinition = "TEXT")
	private String reglasCola;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	@Column(name = "publicado_en")
	private Instant publicadoEn;

	/**
	 * Las zonas del evento.
	 *
	 * 'cascade = ALL' y 'orphanRemoval' hacen que al guardar el evento se
	 * guarden sus zonas, y al borrarlo se borren con el.
	 */
	@OneToMany(mappedBy = "evento", cascade = CascadeType.ALL,
			orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("precio DESC")
	private List<Zona> zonas = new ArrayList<>();

	/**
	 * Crea un evento nuevo en estado BORRADOR.
	 *
	 * Se usa un metodo de fabrica en vez de un constructor publico para que
	 * ningun evento pueda nacer sin id, sin estado o sin fecha de creacion.
	 */
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

	/** True si el evento ya se publico y por tanto esta congelado (RN-01). */
	public boolean estaCongelado() {
		return estado != EstadoEvento.BORRADOR;
	}

	/** Aforo total del recinto: la suma de los aforos de las zonas. */
	public int aforoTotal() {
		return zonas.stream().mapToInt(Zona::getAforo).sum();
	}
}
