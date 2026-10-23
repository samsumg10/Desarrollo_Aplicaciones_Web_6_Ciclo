package com.teleticket.teletickets.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Un orden.pagada que este servicio ya emitio y anuncio. Se escribe con MensajeProcesadoRepository.registrar.
 */
@Entity
@Table(name = "mensaje_procesado")
@Getter
@NoArgsConstructor
public class MensajeProcesado {

	/** El SobreMensaje.eventoId: identifica al mensaje, no a un evento del catalogo. */
	@Id
	@Column(name = "evento_id")
	private UUID eventoId;

	@Column(name = "procesado_en", nullable = false)
	private Instant procesadoEn;
}
