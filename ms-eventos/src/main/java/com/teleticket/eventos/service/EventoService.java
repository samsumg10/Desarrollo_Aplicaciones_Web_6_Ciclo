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

	public EventoService(EventoRepository eventoRepo, ZonaRepository zonaRepo,
			PublicadorEventos publicador) {
		this.eventoRepo = eventoRepo;
		this.zonaRepo = zonaRepo;
		this.publicador = publicador;
	}

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
		if (evento.getZonas().isEmpty()) {
			throw new ReglaNegocioException("SIN_ZONAS",
					"El evento necesita al menos una zona con precio y aforo antes de publicarse");
		}

		evento.setEstado(EstadoEvento.EN_VENTA);
		evento.setPublicadoEn(Instant.now());

		Evento guardado = eventoRepo.saveAndFlush(evento);

		publicador.publicar(Mensajeria.EVENTO_PUBLICADO, SobreMensaje.de(
				Mensajeria.EVENTO_PUBLICADO, requestId, mensajeDe(guardado)));

		return EventoResponse.de(guardado);
	}

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

	@Transactional(readOnly = true)
	public List<EventoResponse> listarPublicados() {
		return eventoRepo.findByEstadoOrderByInicioVentaAsc(EstadoEvento.EN_VENTA)
				.stream().map(EventoResponse::de).toList();
	}

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

	@Transactional(readOnly = true)
	public CondicionesResponse condicionesDeIngreso(UUID eventoId) {
		Evento evento = buscarEvento(eventoId);
		return new CondicionesResponse(
				evento.getId(), evento.getNombre(), evento.getFecha(),
				evento.getRecinto(), evento.getCondicionesIngreso());
	}

	private Evento buscarEvento(UUID eventoId) {
		return eventoRepo.findById(eventoId)
				.orElseThrow(() -> new NoEncontradoException("No existe el evento " + eventoId));
	}

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
