package com.teleticket.apigateway.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.UUID;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Pone un identificador unico a cada peticion que entra al sistema.
 *
 * Por que hace falta: una sola compra pasa por ms-cola, ms-ordenes,
 * ms-inventario, ms-pagos y ms-tickets. Si algo falla, hay que poder seguir
 * esa compra concreta por los logs de los 5 servicios. Con este identificador
 * basta buscar el mismo valor en todos.
 *
 * Si el cliente ya envia su propio X-Request-Id, se respeta. Si no, se genera
 * un UUID. El valor se agrega a la peticion que viaja al microservicio y
 * tambien se devuelve en la respuesta, para que el cliente lo vea.
 *
 * Corresponde a la medida correctiva 2 de Indecopi (corregir errores del
 * portal): sin trazabilidad no se puede corregir lo que no se puede seguir.
 */
@Component
@Order(1) // Primero este: el filtro del pase (orden 2) ya necesita el id en el log.
public class RequestIdFilter implements Filter {

	public static final String CABECERA = "X-Request-Id";

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest peticion = (HttpServletRequest) request;
		HttpServletResponse respuesta = (HttpServletResponse) response;

		String requestId = peticion.getHeader(CABECERA);
		if (requestId == null || requestId.isBlank()) {
			requestId = UUID.randomUUID().toString();
		}

		// Que el cliente lo vea en la respuesta.
		respuesta.setHeader(CABECERA, requestId);

		// La peticion original es inmutable, asi que se envuelve en una copia
		// que si expone la cabecera. Eso es lo que recibe el microservicio.
		chain.doFilter(new PeticionConRequestId(peticion, requestId), respuesta);
	}

	/**
	 * Envoltorio que agrega una cabecera a una peticion que no la traia.
	 */
	private static class PeticionConRequestId extends HttpServletRequestWrapper {

		private final String requestId;

		PeticionConRequestId(HttpServletRequest original, String requestId) {
			super(original);
			this.requestId = requestId;
		}

		@Override
		public String getHeader(String nombre) {
			if (CABECERA.equalsIgnoreCase(nombre)) {
				return requestId;
			}
			return super.getHeader(nombre);
		}

		@Override
		public Enumeration<String> getHeaders(String nombre) {
			if (CABECERA.equalsIgnoreCase(nombre)) {
				return Collections.enumeration(Collections.singletonList(requestId));
			}
			return super.getHeaders(nombre);
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			java.util.List<String> nombres = new java.util.ArrayList<>();
			Enumeration<String> originales = super.getHeaderNames();
			while (originales.hasMoreElements()) {
				nombres.add(originales.nextElement());
			}
			boolean yaEsta = nombres.stream().anyMatch(CABECERA::equalsIgnoreCase);
			if (!yaEsta) {
				nombres.add(CABECERA);
			}
			return Collections.enumeration(nombres);
		}
	}
}
