package com.teleticket.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de registro de un comprador (CU-01).
 *
 * NO incluye 'rol' a proposito: el registro publico siempre crea un COMPRADOR.
 * Si el cliente pudiera elegir el rol, cualquiera se registraria como OPERADOR.
 * El personal se crea por otra via (siembra de datos hoy; Security despues).
 */
public record RegistroRequest(

		@NotBlank(message = "El DNI es obligatorio")
		@Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 digitos")
		String dni,

		@NotBlank(message = "Los nombres son obligatorios")
		@Size(max = 150, message = "Los nombres no pueden pasar de 150 caracteres")
		String nombres,

		@NotBlank(message = "El correo es obligatorio")
		@Email(message = "El correo no tiene un formato valido")
		@Size(max = 150, message = "El correo no puede pasar de 150 caracteres")
		String email,

		// BCrypt solo considera los primeros 72 bytes, por eso el tope.
		@NotBlank(message = "La contrasena es obligatoria")
		@Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
		String password) {
}
