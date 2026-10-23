package com.teleticket.cola.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Un mensaje que este servicio ya atendio. Sirve para descartar los repetidos.
 *
 * Solo se lee a traves de MensajeProcesadoRepository.registrar, que inserta
 * con un SQL propio: asi la comprobacion "ya lo vi?" y el registro son una
 * sola operacion atomica.
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
