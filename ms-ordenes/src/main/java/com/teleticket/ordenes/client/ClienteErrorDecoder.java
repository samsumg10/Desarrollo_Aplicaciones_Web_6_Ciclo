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

public class ClienteErrorDecoder implements ErrorDecoder {

	private static final Logger log = LoggerFactory.getLogger(ClienteErrorDecoder.class);

	private final JsonMapper json;

	public ClienteErrorDecoder(JsonMapper json) {
		this.json = json;
	}

	@Override
	public Exception decode(String methodKey, Response response) {
		String cliente = methodKey.substring(0, methodKey.indexOf('#'));
		String servicio = switch (cliente) {
			case "ColaClient" -> "ms-cola";
			case "EventoClient" -> "ms-eventos";
			case "InventarioClient" -> "ms-inventario";
			case "PagoClient" -> "ms-pagos";
			default -> cliente;
		};

		Map<String, Object> cuerpo = leerCuerpo(response);
		String codigo = String.valueOf(cuerpo.getOrDefault("error", ""));
		String mensaje = String.valueOf(cuerpo.getOrDefault("mensaje", "sin detalle"));

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
