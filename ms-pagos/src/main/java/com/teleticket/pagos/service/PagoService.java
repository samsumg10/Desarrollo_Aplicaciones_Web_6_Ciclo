package com.teleticket.pagos.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.teleticket.pagos.dto.CobrarRequest;
import com.teleticket.pagos.dto.PagoResponse;
import com.teleticket.pagos.dto.ResultadoCobro;
import com.teleticket.pagos.entity.EstadoPago;
import com.teleticket.pagos.entity.Pago;
import com.teleticket.pagos.exception.ReglaNegocioException;
import com.teleticket.pagos.pasarela.PasarelaSimulada;
import com.teleticket.pagos.pasarela.ResultadoPasarela;
import com.teleticket.pagos.repository.PagoRepository;

@Service
public class PagoService {

	private static final Logger log = LoggerFactory.getLogger(PagoService.class);

	private final PagoRepository pagoRepo;
	private final PasarelaSimulada pasarela;
	private final TransactionTemplate tx;

	public PagoService(PagoRepository pagoRepo, PasarelaSimulada pasarela, TransactionTemplate tx) {
		this.pagoRepo = pagoRepo;
		this.pasarela = pasarela;
		this.tx = tx;
	}

	public ResultadoCobro cobrar(CobrarRequest req) {
		String clave = req.ordenId().toString();
		String ultimos4 = req.numeroTarjeta().substring(req.numeroTarjeta().length() - 4);

		UUID idNuevo = UUID.randomUUID();
		Integer insertado = tx.execute(s -> pagoRepo.iniciar(idNuevo, req.ordenId(), req.monto(), ultimos4, clave));

		UUID pagoId = idNuevo;
		if (insertado == 0) {
			Pago existente = pagoRepo.findByIdempotencyKey(clave).orElseThrow();
			pagoId = existente.getId();

			if (existente.getMonto().compareTo(req.monto()) != 0) {
				throw new ReglaNegocioException("PAGO_CONFLICTO",
						"La orden %s ya tiene un pago por %s y se pidio cobrar %s"
								.formatted(req.ordenId(), existente.getMonto(), req.monto()));
			}

			switch (existente.getEstado()) {
				case APROBADO, REEMBOLSADO -> {
					log.info("Orden {}: ya estaba cobrada ({}), se devuelve el mismo pago sin cobrar de nuevo",
							req.ordenId(), existente.getReferenciaPasarela());
					return new ResultadoCobro(PagoResponse.de(existente), false);
				}
				case PENDIENTE -> throw enCurso(req.ordenId());
				case RECHAZADO -> {
					UUID id = existente.getId();
					Integer reabierto = tx.execute(s -> pagoRepo.reintentar(id, ultimos4));
					if (reabierto == 0) {
						throw enCurso(req.ordenId());
					}
					log.info("Orden {}: nuevo intento de cobro tras un rechazo", req.ordenId());
				}
			}
		}

		ResultadoPasarela resultado = pasarela.cobrar(req.numeroTarjeta(), req.monto());

		UUID id = pagoId;
		EstadoPago estado = resultado.aprobado() ? EstadoPago.APROBADO : EstadoPago.RECHAZADO;
		tx.executeWithoutResult(s -> pagoRepo.resolver(id, estado.name(), resultado.referencia(), resultado.motivo()));

		Pago pago = pagoRepo.findById(id).orElseThrow();
		return new ResultadoCobro(PagoResponse.de(pago), true);
	}

	private ReglaNegocioException enCurso(UUID ordenId) {
		return new ReglaNegocioException("PAGO_EN_CURSO",
				"El pago de la orden %s se esta procesando. Consulta de nuevo en unos segundos".formatted(ordenId));
	}
}
