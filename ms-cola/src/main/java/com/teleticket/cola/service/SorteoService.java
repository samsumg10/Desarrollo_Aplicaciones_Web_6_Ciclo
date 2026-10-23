package com.teleticket.cola.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.teleticket.cola.entity.EstadoTurno;
import com.teleticket.cola.entity.VentaEvento;
import com.teleticket.cola.repository.TurnoRepository;
import com.teleticket.cola.repository.VentaEventoRepository;

/**
 * El sorteo unico de cada evento (PRB-02).
 *
 * A la hora anunciada se reparten las posiciones al azar entre todos los que
 * estan en la sala, una sola vez. Despues las posiciones no se tocan nunca mas:
 * es exactamente lo que fallo en 2017, cuando el sistema reordeno a todos desde
 * una hora distinta a la anunciada.
 */
@Service
public class SorteoService {

	private static final Logger log = LoggerFactory.getLogger(SorteoService.class);

	private final VentaEventoRepository ventaRepo;
	private final TurnoRepository turnoRepo;
	private final TransactionTemplate tx;

	public SorteoService(VentaEventoRepository ventaRepo, TurnoRepository turnoRepo, TransactionTemplate tx) {
		this.ventaRepo = ventaRepo;
		this.turnoRepo = turnoRepo;
		this.tx = tx;
	}

	/**
	 * Corre cada segundo (ver TareasProgramadas): sortea los eventos cuya hora ya
	 * llego y acomoda a los que quedaron en la sala durante el sorteo.
	 *
	 * Cada evento va en su propia transaccion: si uno falla, los demas se sortean igual.
	 */
	public void ejecutarPendientes() {
		for (UUID eventoId : ventaRepo.pendientesDeSorteo()) {
			try {
				sortear(eventoId);
			} catch (RuntimeException e) {
				log.error("Fallo el sorteo del evento {}; se reintenta en el proximo ciclo", eventoId, e);
			}
		}
		for (UUID eventoId : ventaRepo.conRezagados()) {
			try {
				ubicarRezagados(eventoId);
			} catch (RuntimeException e) {
				log.error("No se pudo ubicar a los rezagados del evento {}", eventoId, e);
			}
		}
	}

	/**
	 * El sorteo de UN evento, en UNA sola transaccion:
	 *
	 *   1. reclama el sorteo (PENDIENTE -> REALIZADO), que solo UN intento logra;
	 *   2. reparte las posiciones aleatorias a todos los EN_SALA;
	 *   3. deja el contador listo para las llegadas tardias.
	 *
	 * Si algo falla en 2 o 3, el paso 1 se deshace: el evento sigue PENDIENTE y
	 * se vuelve a intentar completo. Nunca queda "sorteado" a medias.
	 */
	void sortear(UUID eventoId) {
		tx.executeWithoutResult(s -> {
			if (ventaRepo.reclamarSorteo(eventoId) == 0) {
				return; // otro intento ya lo hizo: el sorteo es unico
			}
			int sorteados = turnoRepo.sortear(eventoId);
			ventaRepo.fijarUltimaPosicion(eventoId);
			log.info("SORTEO del evento {}: {} personas ordenadas al azar", eventoId, sorteados);
		});
	}

	/**
	 * Quien entro justo mientras corria el sorteo pudo quedar EN_SALA sin posicion
	 * (su ingreso no alcanzo a verse en el sorteo). Se le da la siguiente posicion
	 * libre, por orden de llegada: es la misma regla de la llegada tardia (RN-02).
	 *
	 * Se bloquea la fila del evento para que esto no se cruce con otra llegada
	 * tardia que este tomando posicion en ese mismo instante.
	 */
	void ubicarRezagados(UUID eventoId) {
		tx.executeWithoutResult(s -> {
			VentaEvento venta = ventaRepo.bloquear(eventoId).orElseThrow();
			var rezagados = turnoRepo.findByEventoIdAndEstadoOrderByIngresoEnAscIdAsc(eventoId, EstadoTurno.EN_SALA);
			rezagados.forEach(t -> t.ubicarEnCola(venta.siguientePosicion()));
			if (!rezagados.isEmpty()) {
				log.info("Evento {}: {} persona(s) que entraron durante el sorteo se ubican al final",
						eventoId, rezagados.size());
			}
		});
	}
}
