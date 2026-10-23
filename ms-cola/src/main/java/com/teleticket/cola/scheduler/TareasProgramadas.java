package com.teleticket.cola.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.teleticket.cola.service.AdmisionService;
import com.teleticket.cola.service.SorteoService;

@Component
public class TareasProgramadas {

	private final SorteoService sorteo;
	private final AdmisionService admision;

	public TareasProgramadas(SorteoService sorteo, AdmisionService admision) {
		this.sorteo = sorteo;
		this.admision = admision;
	}

	@Scheduled(fixedDelayString = "1s")
	public void sortear() {
		sorteo.ejecutarPendientes();
	}

	@Scheduled(fixedDelayString = "${teleticket.cola.ciclo-segundos:10}s")
	public void admitir() {
		admision.ejecutarCiclo();
	}
}
