package com.teleticket.eventos.dto;

import java.time.Instant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Lo que el operador envia para crear un evento (CU-14).
 *
 * Es un 'record': una clase de solo lectura con menos ceremonia que una clase
 * normal. Java genera el constructor y los getters.
 *
 * Por que un DTO y no la entidad Evento directamente: asi el cliente no puede
 * mandar campos que no le corresponden (como 'estado' o 'id') ni ver la
 * estructura interna de la base de datos. Las anotaciones @NotBlank, @Min...
 * se validan ANTES de tocar la base, y si fallan Spring responde 400 con el
 * detalle de cada campo.
 */
public record CrearEventoRequest(

		@NotBlank(message = "El nombre del evento es obligatorio")
		@Size(max = 200, message = "El nombre no puede pasar de 200 caracteres")
		String nombre,

		@NotNull(message = "La fecha del evento es obligatoria")
		Instant fecha,

		@NotBlank(message = "El recinto es obligatorio")
		@Size(max = 200, message = "El recinto no puede pasar de 200 caracteres")
		String recinto,

		@NotNull(message = "La hora de inicio de venta es obligatoria")
		Instant inicioVenta,

		@NotNull(message = "El maximo por cliente es obligatorio")
		@Min(value = 1, message = "El maximo por cliente debe ser al menos 1")
		@Max(value = 20, message = "El maximo por cliente no puede pasar de 20")
		Integer maxPorCliente,

		/**
		 * Lo que se exige para entrar. Viaja a cada e-ticket y a cada correo,
		 * para que lo informado sea lo que se valida (PRB-06).
		 */
		@NotBlank(message = "Las condiciones de ingreso son obligatorias: lo informado es lo que se valida en la puerta")
		String condicionesIngreso,

		/** Reglas de la cola virtual. Opcional: si no se envia, se usa un texto por defecto. */
		String reglasCola) {
}
