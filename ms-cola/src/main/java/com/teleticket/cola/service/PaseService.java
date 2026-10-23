package com.teleticket.cola.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teleticket.cola.dto.PaseConsumidoResponse;
import com.teleticket.cola.exception.ReglaNegocioException;
import com.teleticket.cola.repository.PaseCompraRepository;

@Service
public class PaseService {

	private final PaseCompraRepository paseRepo;

	public PaseService(PaseCompraRepository paseRepo) {
		this.paseRepo = paseRepo;
	}

	@Transactional
	public PaseConsumidoResponse consumir(UUID codigo, UUID usuarioId, UUID eventoId) {
		int filas = eventoId == null
				? paseRepo.consumir(codigo, usuarioId)
				: paseRepo.consumirDeEvento(codigo, usuarioId, eventoId);
		if (filas == 0) {
			throw new ReglaNegocioException("PASE_NO_VALIDO",
					"El pase no es valido: no existe, es de otra persona o de otro evento, ya se uso o vencio");
		}
		return PaseConsumidoResponse.de(paseRepo.findById(codigo).orElseThrow());
	}

	@Transactional
	public PaseConsumidoResponse devolver(UUID codigo, UUID usuarioId) {
		if (paseRepo.devolver(codigo, usuarioId) == 0) {
			throw new ReglaNegocioException("PASE_NO_DEVOLVIBLE",
					"El pase no se puede devolver: no esta usado, es de otra persona o ya vencio");
		}
		return PaseConsumidoResponse.de(paseRepo.findById(codigo).orElseThrow());
	}
}
