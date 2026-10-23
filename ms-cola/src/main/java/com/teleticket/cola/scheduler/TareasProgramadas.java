package com.teleticket.cola.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.teleticket.cola.service.AdmisionService;
import com.teleticket.cola.service.SorteoService;

/**
 * Los dos relojes de la sala de espera.
 *
 * Solo disparan: toda la logica esta en los servicios, que se pueden invocar
 * igual desde una prueba sin esperar al reloj.
 *
 * Con varias replicas de ms-cola correrian las dos tareas en cada una; es seguro
 * porque el sorteo se reclama con un UPDATE condicional y la admision toma los
 * turnos con FOR UPDATE SKIP LOCKED (ver los repositorios).
 */
@Component
public class TareasProgramadas {

	private final SorteoService sorteo;
	private final AdmisionService admision;

	public TareasProgramadas(SorteoService sorteo, AdmisionService admision) {
		this.sorteo = sorteo;
		this.admision = admision;
	}

	/**
	 * Cada segundo mira si llego la hora de algun sorteo. Un segundo es el margen
	 * maximo con el que el sorteo ocurre despues de la hora anunciada.
	 */
	@Scheduled(fixedDelayString = "1s")
	public void sortear() {
		sorteo.ejecutarPendientes();
	}

	/**
	 * Cada 'ciclo-segundos' (10 por defecto) habilita al siguiente grupo.
	 * 'fixedDelay' cuenta desde que termina la anterior: dos ciclos nunca se pisan.
	 */
	@Scheduled(fixedDelayString = "${teleticket.cola.ciclo-segundos:10}s")
	public void admitir() {
		admision.ejecutarCiclo();
	}
}
