package com.teleticket.cola.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mensaje_procesado")
@Getter
@NoArgsConstructor
public class MensajeProcesado {

	@Id
	@Column(name = "evento_id")
	private UUID eventoId;

	@Column(name = "procesado_en", nullable = false)
	private Instant procesadoEn;
}
