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

/**
 * La copia local de un evento: lo unico que ms-cola necesita saber de el.
 *
 * Se crea al recibir evento.publicado. Sin setters: el estado del sorteo y el
 * contador de posiciones cambian con UPDATE condicionales, para que dos hilos
 * (o dos replicas del servicio) no se pisen. La unica excepcion es
 * siguientePosicion(), que solo se usa con la fila bloqueada (FOR UPDATE).
 */
@Entity
@Table(name = "venta_evento")
@Getter
@NoArgsConstructor
public class VentaEvento {

	/** El mismo id que el evento tiene en ms-eventos. */
	@Id
	@Column(name = "evento_id")
	private UUID eventoId;

	@Column(nullable = false, length = 200)
	private String nombre;

	/** Hora anunciada del sorteo. Congelada en ms-eventos desde que se publico (RN-01). */
	@Column(name = "inicio_venta", nullable = false)
	private Instant inicioVenta;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_sorteo", nullable = false, length = 12)
	private EstadoSorteo estadoSorteo;

	/** Cuantos turnos se habilitan en cada ciclo de admision. */
	@Column(name = "capacidad_por_ciclo", nullable = false)
	private Integer capacidadPorCiclo;

	/** La ultima posicion entregada. La siguiente persona que llegue tarde recibe esta + 1. */
	@Column(name = "ultima_posicion", nullable = false)
	private Integer ultimaPosicion;

	@Column(name = "sorteo_en")
	private Instant sorteoEn;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	/** Entrega la siguiente posicion. Solo con la fila bloqueada: ver TurnoRepository / SorteoService. */
	public int siguientePosicion() {
		ultimaPosicion = ultimaPosicion + 1;
		return ultimaPosicion;
	}
}
