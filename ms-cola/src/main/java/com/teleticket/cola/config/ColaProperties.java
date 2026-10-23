package com.teleticket.cola.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "teleticket.cola")
public record ColaProperties(
		@DefaultValue("20") int capacidadPorCiclo,
		@DefaultValue("10") int cicloSegundos,
		@DefaultValue("10") int paseMinutos) {
}
