package com.teleticket.eventos.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teleticket.eventos.dto.CondicionesResponse;
import com.teleticket.eventos.dto.CrearEventoRequest;
import com.teleticket.eventos.dto.CrearZonaRequest;
import com.teleticket.eventos.dto.EventoResponse;
import com.teleticket.eventos.dto.ZonaInternaResponse;
import com.teleticket.eventos.dto.ZonaResponse;
import com.teleticket.eventos.entity.EstadoEvento;
import com.teleticket.eventos.entity.Evento;
import com.teleticket.eventos.entity.Zona;
import com.teleticket.eventos.exception.NoEncontradoException;
import com.teleticket.eventos.exception.ReglaNegocioException;
import com.teleticket.eventos.repository.EventoRepository;
import com.teleticket.eventos.repository.ZonaRepository;
import com.teleticket.mensajeria.Mensajeria;
import com.teleticket.mensajeria.PublicadorEventos;
import com.teleticket.mensajeria.SobreMensaje;
import com.teleticket.mensajeria.mensajes.EventoPublicado;
import com.teleticket.mensajeria.mensajes.ZonaPublicada;

/**
 * Las reglas de negocio del catalogo de eventos.
 *
 * Esta clase es donde viven las decisiones; el controlador solo recibe la
 * peticion y la pasa aqui. Separarlo asi permite cambiar la API sin tocar las
 * reglas, y probar las reglas sin levantar un servidor web.
 *
 * Reglas que implementa:
 *   RN-01  La hora de venta y las reglas de la cola se congelan al publicar.
 *   RN-05  El maximo por comprador se define aqui y lo aplica ms-ordenes.
 *   RN-14  Las condiciones de ingreso viajan al e-ticket y al correo.
 *
 * Publica el mensaje evento.publicado (fase 5) a traves de PublicadorEventos:
 * esta clase no sabe si por detras hay RabbitMQ o Kafka.
 */
@Service
public class EventoService {

	private static final String REGLAS_COLA_POR_DEFECTO = """
			1. Debes iniciar sesion para entrar a la sala de espera.
			2. A la hora anunciada se realiza un unico sorteo que asigna una posicion \
			aleatoria a todos los que esten en la sala.
			3. Quien entre despues del sorteo se ubica al final, por orden de llegada.
			4. Cada 10 segundos se habilita a un grupo de compradores y se le entrega \
			un pase de compra personal, de un solo uso, valido por 10 minutos.
			5. Sin pase no se puede comprar. Si el pase vence sin usarse, el turno \
			pasa al siguiente comprador.""";

	private final EventoRepository eventoRepo;
	private final ZonaRepository zonaRepo;
	private final PublicadorEventos publicador;

	/**
	 * Spring inyecta las dependencias por el constructor.
	 *
	 * Se usa constructor y no @Autowired en el campo porque asi los campos
	 * pueden ser 'final' y la clase no puede existir a medio construir.
	 */
	public EventoService(EventoRepository eventoRepo, ZonaRepository zonaRepo,
			PublicadorEventos publicador) {
		this.eventoRepo = eventoRepo;
		this.zonaRepo = zonaRepo;
		this.publicador = publicador;
	}

	// =================================================================
	// CU-14: configurar el evento
	// =================================================================

	@Transactional
	public EventoResponse crear(CrearEventoRequest req) {
		validarFechas(req.inicioVenta(), req.fecha());

		String reglas = (req.reglasCola() == null || req.reglasCola().isBlank())
				? REGLAS_COLA_POR_DEFECTO
				: req.reglasCola();

		Evento evento = Evento.crear(
				req.nombre().trim(),
				req.fecha(),
				req.recinto().trim(),
				req.inicioVenta(),
				req.maxPorCliente(),
				req.condicionesIngreso().trim(),
				reglas);

		return EventoResponse.de(eventoRepo.save(evento));
	}

	@Transactional
	public ZonaResponse agregarZona(UUID eventoId, CrearZonaRequest req) {
		Evento evento = buscarEvento(eventoId);

		// RN-01: si ya se publico, el evento esta congelado.
		if (evento.estaCongelado()) {
			throw new ReglaNegocioException("EVENTO_CONGELADO",
					("No se pueden agregar zonas a un evento en estado %s. "
							+ "Las zonas, la hora de venta y las reglas quedan fijas al publicar el evento.")
							.formatted(evento.getEstado()));
		}

		String nombre = req.nombre().trim();
		if (zonaRepo.existsByEventoIdAndNombreIgnoreCase(eventoId, nombre)) {
			throw new ReglaNegocioException("ZONA_DUPLICADA",
					"El evento ya tiene una zona llamada '%s'".formatted(nombre));
		}

		Zona zona = Zona.crear(nombre, req.precio(), req.aforo());
		evento.agregarZona(zona);
		eventoRepo.save(evento);

		return ZonaResponse.de(zona);
	}

	// =================================================================
	// CU-15: publicar reglas y horario
	// =================================================================

	/**
	 * Publica el evento: pasa a EN_VENTA y queda congelado para siempre.
	 *
	 * Es el momento mas importante del ciclo de vida, porque a partir de aqui
	 * la hora de venta y las reglas que vio el comprador ya no pueden cambiar.
	 * Eso es exactamente lo que fallo en 2017.
	 *
	 * Ademas publica evento.publicado, que hace que ms-cola prepare la sala de
	 * espera y ms-inventario cree el stock de cada zona.
	 *
	 * El mensaje se publica como ULTIMO paso, despues de guardar y antes del
	 * commit: si RabbitMQ no confirma, se lanza una excepcion, la transaccion
	 * se deshace y el evento sigue en BORRADOR. Asi nunca queda un evento
	 * EN_VENTA del que nadie se entero. El caso contrario (el mensaje sale y
	 * el commit falla justo despues) es posible pero muy raro; cerrarlo del todo
	 * exige el patron "outbox", que queda fuera del alcance del proyecto.
	 *
	 * @param requestId el X-Request-Id de la peticion; viaja en el mensaje como
	 *                  correlationId para poder seguirlo por los logs
	 */
	@Transactional
	public EventoResponse publicar(UUID eventoId, String requestId) {
		Evento evento = eventoRepo.buscarConZonas(eventoId)
				.orElseThrow(() -> new NoEncontradoException(
						"No existe el evento " + eventoId));

		if (evento.getEstado() == EstadoEvento.EN_VENTA) {
			throw new ReglaNegocioException("YA_PUBLICADO",
					"El evento ya esta EN_VENTA desde " + evento.getPublicadoEn());
		}
		if (evento.estaCongelado()) {
			throw new ReglaNegocioException("ESTADO_INVALIDO",
					"No se puede publicar un evento en estado " + evento.getEstado());
		}
		// Sin zonas no hay nada que vender, y ms-inventario no podria crear stock.
		if (evento.getZonas().isEmpty()) {
			throw new ReglaNegocioException("SIN_ZONAS",
					"El evento necesita al menos una zona con precio y aforo antes de publicarse");
		}

		evento.setEstado(EstadoEvento.EN_VENTA);
		evento.setPublicadoEn(Instant.now());

		// saveAndFlush: que la base valide el cambio ANTES de avisar a nadie.
		Evento guardado = eventoRepo.saveAndFlush(evento);

		publicador.publicar(Mensajeria.EVENTO_PUBLICADO, SobreMensaje.de(
				Mensajeria.EVENTO_PUBLICADO, requestId, mensajeDe(guardado)));

		return EventoResponse.de(guardado);
	}

	/**
	 * Modifica un evento que todavia esta en BORRADOR.
	 *
	 * Aqui se ve RN-01 con toda claridad: el mismo cambio es valido antes de
	 * publicar y prohibido despues.
	 */
	@Transactional
	public EventoResponse actualizar(UUID eventoId, CrearEventoRequest req) {
		Evento evento = buscarEvento(eventoId);

		if (evento.estaCongelado()) {
			throw new ReglaNegocioException("EVENTO_CONGELADO",
					("No se puede modificar la hora de venta ni las reglas de un evento %s. "
							+ "Se publicaron al comprador y cambiarlas seria repetir el problema de 2017.")
							.formatted(evento.getEstado()));
		}

		validarFechas(req.inicioVenta(), req.fecha());

		evento.setNombre(req.nombre().trim());
		evento.setFecha(req.fecha());
		evento.setRecinto(req.recinto().trim());
		evento.setInicioVenta(req.inicioVenta());
		evento.setMaxPorCliente(req.maxPorCliente());
		evento.setCondicionesIngreso(req.condicionesIngreso().trim());
		if (req.reglasCola() != null && !req.reglasCola().isBlank()) {
			evento.setReglasCola(req.reglasCola());
		}

		return EventoResponse.de(eventoRepo.save(evento));
	}

	// =================================================================
	// CU-02: consultar eventos
	// =================================================================

	/** Solo los eventos EN_VENTA: los borradores no se muestran al publico. */
	@Transactional(readOnly = true)
	public List<EventoResponse> listarPublicados() {
		return eventoRepo.findByEstadoOrderByInicioVentaAsc(EstadoEvento.EN_VENTA)
				.stream().map(EventoResponse::de).toList();
	}

	/** Todos, incluidos los borradores. Para el operador. */
	@Transactional(readOnly = true)
	public List<EventoResponse> listarTodos() {
		return eventoRepo.findAll().stream().map(EventoResponse::de).toList();
	}

	@Transactional(readOnly = true)
	public EventoResponse verDetalle(UUID eventoId) {
		return EventoResponse.de(eventoRepo.buscarConZonas(eventoId)
				.orElseThrow(() -> new NoEncontradoException(
						"No existe el evento " + eventoId)));
	}

	// =================================================================
	// API interna: la consumen otros microservicios por Feign
	// =================================================================

	/** Lo llama ms-ordenes para saber precio y limite antes de crear la orden (fase 8). */
	@Transactional(readOnly = true)
	public ZonaInternaResponse datosDeZona(UUID eventoId, UUID zonaId) {
		Zona zona = zonaRepo.findByIdAndEventoId(zonaId, eventoId)
				.orElseThrow(() -> new NoEncontradoException(
						"No existe la zona %s en el evento %s".formatted(zonaId, eventoId)));

		Evento evento = zona.getEvento();
		boolean ventaAbierta = evento.getEstado() == EstadoEvento.EN_VENTA
				&& !Instant.now().isBefore(evento.getInicioVenta());

		return new ZonaInternaResponse(
				zona.getId(), evento.getId(), zona.getNombre(), zona.getPrecio(),
				zona.getAforo(), evento.getMaxPorCliente(), ventaAbierta);
	}

	/** Lo llama ms-tickets para copiar las condiciones en cada e-ticket (fase 10). */
	@Transactional(readOnly = true)
	public CondicionesResponse condicionesDeIngreso(UUID eventoId) {
		Evento evento = buscarEvento(eventoId);
		return new CondicionesResponse(
				evento.getId(), evento.getNombre(), evento.getFecha(),
				evento.getRecinto(), evento.getCondicionesIngreso());
	}

	// -----------------------------------------------------------------

	private Evento buscarEvento(UUID eventoId) {
		return eventoRepo.findById(eventoId)
				.orElseThrow(() -> new NoEncontradoException("No existe el evento " + eventoId));
	}

	/** Todo lo que ms-cola y ms-inventario necesitan, para que no tengan que llamarnos. */
	private EventoPublicado mensajeDe(Evento e) {
		List<ZonaPublicada> zonas = e.getZonas().stream()
				.map(z -> new ZonaPublicada(z.getId(), z.getNombre(), z.getPrecio(), z.getAforo()))
				.toList();
		return new EventoPublicado(e.getId(), e.getNombre(), e.getFecha(), e.getRecinto(),
				e.getInicioVenta(), e.getMaxPorCliente(), e.getReglasCola(), zonas);
	}

	private void validarFechas(Instant inicioVenta, Instant fechaEvento) {
		if (!inicioVenta.isBefore(fechaEvento)) {
			throw new ReglaNegocioException("FECHAS_INCOHERENTES",
					"La venta debe empezar antes de la fecha del evento");
		}
	}
}
