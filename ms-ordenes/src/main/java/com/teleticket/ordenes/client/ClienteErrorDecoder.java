package com.teleticket.ordenes.client;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.teleticket.ordenes.exception.NoEncontradoException;
import com.teleticket.ordenes.exception.PaseNoValidoException;
import com.teleticket.ordenes.exception.ReglaNegocioException;
import com.teleticket.ordenes.exception.ServicioNoDisponibleException;
import com.teleticket.ordenes.exception.SinStockException;

import feign.Response;
import feign.codec.ErrorDecoder;
import tools.jackson.databind.json.JsonMapper;

/**
 * Traduce lo que responde otro servicio cuando algo sale mal a una excepcion propia.
 *
 * Todos los servicios responden los errores con el mismo JSON:
 * {"error": "SIN_STOCK", "mensaje": "...", "requestId": "..."}. Aqui se lee el codigo
 * y se decide:
 *
 *   404                          -> NoEncontradoException
 *   409 PASE_NO_VALIDO           -> PaseNoValidoException
 *   409 SIN_STOCK                -> SinStockException
 *   409 cualquier otro codigo    -> ReglaNegocioException con ese mismo codigo
 *   cualquier otra cosa (5xx...) -> ServicioNoDisponibleException
 *
 * Asi OrdenService no mira codigos HTTP ni textos: reacciona a tipos. Un servicio
 * que RESPONDE "no" (409) es distinto de uno que NO RESPONDE (503), y de eso depende
 * si hay que devolver el pase.
 *
 * Los timeouts y las conexiones rechazadas no pasan por aqui: Feign los lanza como
 * RetryableException y OrdenService los convierte en ServicioNoDisponibleException.
 */
public class ClienteErrorDecoder implements ErrorDecoder {

	private static final Logger log = LoggerFactory.getLogger(ClienteErrorDecoder.class);

	private final JsonMapper json;

	public ClienteErrorDecoder(JsonMapper json) {
		this.json = json;
	}

	@Override
	public Exception decode(String methodKey, Response response) {
		// methodKey es "ColaClient#consumirPase(String,ConsumirPaseRequest)": el cliente es lo de antes del '#'.
		String cliente = methodKey.substring(0, methodKey.indexOf('#'));
		String servicio = switch (cliente) {
			case "ColaClient" -> "ms-cola";
			case "EventoClient" -> "ms-eventos";
			case "InventarioClient" -> "ms-inventario";
			default -> cliente;
		};

		Map<String, Object> cuerpo = leerCuerpo(response);
		String codigo = String.valueOf(cuerpo.getOrDefault("error", ""));
		String mensaje = String.valueOf(cuerpo.getOrDefault("mensaje", "sin detalle"));

		// El requestId que devuelve el otro servicio es el que le mando yo (RequestIdInterceptor):
		// si coincide con el de esta peticion, el rastro de la compra esta completo en los logs.
		log.info("{} respondio {} {} (requestId={})", servicio, response.status(), codigo, cuerpo.get("requestId"));

		int estado = response.status();
		if (estado == 404) {
			return new NoEncontradoException(mensaje);
		}
		if (estado == 409) {
			return switch (codigo) {
				case "PASE_NO_VALIDO" -> new PaseNoValidoException(mensaje);
				case "SIN_STOCK" -> new SinStockException(mensaje);
				default -> new ReglaNegocioException(codigo, mensaje);
			};
		}
		return new ServicioNoDisponibleException(servicio, "respondio " + estado + " " + codigo, null);
	}

	/** Lee el JSON de error; si no hay cuerpo o no es JSON, devuelve un mapa vacio. */
	@SuppressWarnings("unchecked")
	private Map<String, Object> leerCuerpo(Response response) {
		if (response.body() == null) {
			return Map.of();
		}
		try (var in = response.body().asInputStream()) {
			return json.readValue(in, Map.class);
		} catch (IOException | RuntimeException e) {
			return Map.of();
		}
	}
}
