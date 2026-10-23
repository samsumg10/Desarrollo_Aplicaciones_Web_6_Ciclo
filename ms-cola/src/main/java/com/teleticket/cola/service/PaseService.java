package com.teleticket.cola.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teleticket.cola.dto.PaseConsumidoResponse;
import com.teleticket.cola.exception.ReglaNegocioException;
import com.teleticket.cola.repository.PaseCompraRepository;

/**
 * Consumo del pase de compra. Lo llama ms-ordenes por Feign al crear la orden (fase 8).
 */
@Service
public class PaseService {

	private final PaseCompraRepository paseRepo;

	public PaseService(PaseCompraRepository paseRepo) {
		this.paseRepo = paseRepo;
	}

	/**
	 * Consume el pase: un pase, una compra (RN-03, PRB-03).
	 *
	 * La unica decision la toma el UPDATE condicional de la base. Si afecta 0 filas
	 * el pase no existe, es de otra persona, ya se uso o vencio. Todos esos casos
	 * dan el mismo error a proposito: quien prueba codigos al azar no debe poder
	 * distinguir "no existe" de "existe pero es de otro".
	 */
	@Transactional
	public PaseConsumidoResponse consumir(UUID codigo, UUID usuarioId) {
		if (paseRepo.consumir(codigo, usuarioId) == 0) {
			throw new ReglaNegocioException("PASE_NO_VALIDO",
					"El pase no es valido: no existe, es de otra persona, ya se uso o vencio");
		}
		return PaseConsumidoResponse.de(paseRepo.findById(codigo).orElseThrow());
	}
}
