package com.teleticket.usuarios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cifrado de contrasenas con BCrypt.
 *
 * BCrypt no es reversible y genera un hash distinto cada vez (lleva una "sal"
 * aleatoria), asi que dos usuarios con la misma contrasena guardan hashes
 * distintos. Para comprobar una contrasena se usa encoder.matches(), no se compara.
 */
@Configuration
public class SeguridadConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
