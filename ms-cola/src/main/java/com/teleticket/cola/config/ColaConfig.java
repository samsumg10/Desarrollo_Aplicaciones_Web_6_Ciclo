package com.teleticket.cola.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa las tareas programadas (el sorteo y la admision) y los parametros de la sala.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(ColaProperties.class)
public class ColaConfig {
}
