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

	/**
	 * Devuelve un pase que se consumio pero no llego a convertirse en orden.
	 *
	 * Es la compensacion de la creacion de ordenes: si ms-ordenes consume el pase y luego
	 * no consigue stock, el comprador no debe perder su turno. Solo lo llama ms-ordenes,
	 * y solo funciona con un pase USADO, de ese usuario y que aun no vencio.
	 */
	@Transactional
	public PaseConsumidoResponse devolver(UUID codigo, UUID usuarioId) {
		if (paseRepo.devolver(codigo, usuarioId) == 0) {
			throw new ReglaNegocioException("PASE_NO_DEVOLVIBLE",
					"El pase no se puede devolver: no esta usado, es de otra persona o ya vencio");
		}
		return PaseConsumidoResponse.de(paseRepo.findById(codigo).orElseThrow());
	}
}
