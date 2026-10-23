package com.teleticket.apigateway.filter;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rechaza cualquier intento de crear una orden sin pase de compra.
 *
 * Esta es la PRIMERA de las dos barreras contra PRB-03 (saltarse la cola):
 *
 *   1. Esta: el Gateway exige que venga la cabecera X-Pase-Compra.
 *   2. La real (fase 8): ms-ordenes valida por Feign contra ms-cola que el
 *      pase exista, sea de ese usuario, no haya vencido y no se haya usado.
 *
 * Esta barrera sola no alcanza (cualquiera puede inventar un valor), pero
 * frena las peticiones obviamente invalidas antes de que lleguen al negocio.
 *
 * Solo se aplica a POST /api/ordenes, que es crear la orden. NO se aplica a
 * POST /api/ordenes/{id}/pagar, porque pagar una orden que ya existe no
 * necesita pase: el pase se consumio al crearla.
 */
@Component
@Order(2)
public class PaseCompraFilter implements Filter {

	public static final String CABECERA = "X-Pase-Compra";

	private static final String RUTA_CREAR_ORDEN = "/api/ordenes";

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest peticion = (HttpServletRequest) request;
		HttpServletResponse respuesta = (HttpServletResponse) response;

		if (esCrearOrden(peticion) && faltaElPase(peticion)) {
			rechazar(peticion, respuesta);
			return;
		}

		chain.doFilter(request, response);
	}

	/**
	 * Solo POST a /api/ordenes exactamente (con o sin barra final).
	 * Las subrutas como /api/ordenes/{id}/pagar quedan fuera a proposito.
	 */
	private boolean esCrearOrden(HttpServletRequest peticion) {
		if (!"POST".equalsIgnoreCase(peticion.getMethod())) {
			return false;
		}
		String ruta = peticion.getRequestURI();
		return RUTA_CREAR_ORDEN.equals(ruta) || (RUTA_CREAR_ORDEN + "/").equals(ruta);
	}

	private boolean faltaElPase(HttpServletRequest peticion) {
		String pase = peticion.getHeader(CABECERA);
		return pase == null || pase.isBlank();
	}

	private void rechazar(HttpServletRequest peticion, HttpServletResponse respuesta) throws IOException {
		String requestId = peticion.getHeader(RequestIdFilter.CABECERA);

		respuesta.setStatus(HttpStatus.BAD_REQUEST.value());
		respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
		respuesta.setCharacterEncoding("UTF-8");
		respuesta.getWriter().write("""
				{
				  "error": "PASE_REQUERIDO",
				  "mensaje": "Para comprar necesitas un pase de compra valido. Entra a la sala de espera y aguarda tu turno.",
				  "detalle": "Falta la cabecera %s",
				  "requestId": "%s"
				}""".formatted(CABECERA, requestId));
	}
}
