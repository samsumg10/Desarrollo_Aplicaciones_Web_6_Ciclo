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

/**
 * Cobrar una orden (CU-06) sin poder cobrarla dos veces.
 *
 * <h3>Idempotencia</h3>
 * La clave es el ordenId. Cobrar es en tres pasos, y el primero es el que hace segura la operacion:
 *
 *   1. RESERVAR la clave: INSERT del pago en PENDIENTE. UNIQUE(idempotency_key) deja pasar a UNA
 *      sola peticion, aunque lleguen cien a la vez.
 *   2. Cobrar en la pasarela (puede tardar; va FUERA de toda transaccion).
 *   3. ANOTAR el resultado: APROBADO o RECHAZADO.
 *
 * Si la clave ya existia, no se cobra otra vez:
 *   - APROBADO  -> se devuelve ese mismo pago (lo que pasa si ms-ordenes reintenta tras un timeout).
 *   - RECHAZADO -> es un nuevo intento (otra tarjeta): la misma fila vuelve a PENDIENTE.
 *   - PENDIENTE -> otra peticion lo esta cobrando ahora mismo: 409 PAGO_EN_CURSO.
 *
 * Reservar la clave ANTES de cobrar (y no buscar, cobrar e insertar al final) es lo que evita el doble
 * cobro cuando dos peticiones coinciden: con "buscar primero", las dos no encuentran nada y las dos cobran.
 *
 * Limitacion conocida: si el servicio se cae entre 1 y 3, el pago queda PENDIENTE y los reintentos reciben
 * PAGO_EN_CURSO. Resolver esos pagos huerfanos sigue pendiente: la fase 11 solo reembolsa pagos APROBADOS.
 */
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

	/**
	 * No es @Transactional a proposito: la pasarela puede tardar segundos y no se debe mantener
	 * una conexion de la base de datos abierta mientras tanto. Cada escritura va en su propia
	 * transaccion corta.
	 */
	public ResultadoCobro cobrar(CobrarRequest req) {
		String clave = req.ordenId().toString();
		String ultimos4 = req.numeroTarjeta().substring(req.numeroTarjeta().length() - 4);

		// 1. Reservar la clave.
		UUID idNuevo = UUID.randomUUID();
		Integer insertado = tx.execute(s -> pagoRepo.iniciar(idNuevo, req.ordenId(), req.monto(), ultimos4, clave));

		UUID pagoId = idNuevo;
		if (insertado == 0) {
			Pago existente = pagoRepo.findByIdempotencyKey(clave).orElseThrow();
			pagoId = existente.getId();

			// La misma orden con otro monto no es un reintento: es una peticion distinta con la misma clave.
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

		// 2. Cobrar en la pasarela. Aqui es donde la tarjeta 9999 hace esperar 5 segundos.
		ResultadoPasarela resultado = pasarela.cobrar(req.numeroTarjeta(), req.monto());

		// 3. Anotar el resultado.
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
