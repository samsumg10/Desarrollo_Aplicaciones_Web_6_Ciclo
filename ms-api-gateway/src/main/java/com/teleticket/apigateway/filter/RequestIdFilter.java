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

@Component
@Order(1)
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

		respuesta.setHeader(CABECERA, requestId);

		chain.doFilter(new PeticionConRequestId(peticion, requestId), respuesta);
	}

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
