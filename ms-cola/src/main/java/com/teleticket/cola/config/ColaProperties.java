package com.teleticket.cola.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Los parametros de la sala de espera, leidos de application.properties
 * (prefijo teleticket.cola). Se pueden cambiar para acelerar las pruebas sin
 * tocar codigo.
 *
 * @param capacidadPorCiclo cuantos turnos se habilitan en cada ciclo
 * @param cicloSegundos     cada cuantos segundos corre la admision
 * @param paseMinutos       cuanto vale un pase de compra
 */
@ConfigurationProperties(prefix = "teleticket.cola")
public record ColaProperties(
		@DefaultValue("20") int capacidadPorCiclo,
		@DefaultValue("10") int cicloSegundos,
		@DefaultValue("10") int paseMinutos) {
}
