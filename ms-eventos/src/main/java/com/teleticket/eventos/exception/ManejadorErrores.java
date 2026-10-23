package com.teleticket.eventos.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.teleticket.mensajeria.PublicacionFallidaException;

/**
 * Convierte las excepciones en respuestas JSON claras.
 *
 * Sin esto, un error devuelve una pagina de error de Spring o un volcado de
 * pila, que no sirve ni al comprador ni a quien depura. Con esto, cada error
 * sale con el codigo HTTP correcto y un cuerpo siempre igual:
 *
 *   {
 *     "error": "EVENTO_CONGELADO",
 *     "mensaje": "No se puede modificar...",
 *     "estado": 409,
 *     "ruta": "/api/eventos/...",
 *     "requestId": "a8b308ed-...",
 *     "fecha": "2026-10-23T03:35:45Z"
 *   }
 *
 * El requestId es el que puso el Gateway (fase 2): permite buscar esta misma
 * peticion en los logs de todos los servicios por los que paso.
 */
@RestControllerAdvice
public class ManejadorErrores {

	/** Lo pidieron y no existe -> 404. */
	@ExceptionHandler(NoEncontradoException.class)
	public ResponseEntity<Map<String, Object>> noEncontrado(NoEncontradoException ex, WebRequest req) {
		return respuesta(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", ex.getMessage(), req);
	}

	/** Una regla del negocio lo prohibe -> 409. */
	@ExceptionHandler(ReglaNegocioException.class)
	public ResponseEntity<Map<String, Object>> reglaNegocio(ReglaNegocioException ex, WebRequest req) {
		return respuesta(HttpStatus.CONFLICT, ex.getCodigo(), ex.getMessage(), req);
	}

	/** JSON mal formado o tipos incorrectos -> 400 (sin esto caeria en el 500 generico). */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, Object>> jsonInvalido(HttpMessageNotReadableException ex, WebRequest req) {
		return respuesta(HttpStatus.BAD_REQUEST, "JSON_INVALIDO",
				"El cuerpo de la peticion no es un JSON valido", req);
	}

	/**
	 * RabbitMQ no confirmo el mensaje -> 503.
	 *
	 * No es culpa del cliente ni de una regla de negocio: falta una pieza de
	 * la que depende la operacion. La transaccion ya se deshizo, asi que se
	 * puede reintentar sin riesgo cuando el broker vuelva.
	 */
	@ExceptionHandler(PublicacionFallidaException.class)
	public ResponseEntity<Map<String, Object>> brokerNoDisponible(PublicacionFallidaException ex, WebRequest req) {
		System.err.println("[ms-eventos] " + ex.getMessage() + (ex.getCause() != null ? " <- " + ex.getCause() : ""));
		return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "BROKER_NO_DISPONIBLE",
				"No se pudo avisar al resto del sistema. El evento NO se publico; reintenta en unos minutos.", req);
	}

	/**
	 * Fallaron las validaciones del DTO (@NotBlank, @Min...) -> 400.
	 *
	 * Se devuelve la lista campo por campo, para que el cliente sepa
	 * exactamente que corregir en vez de un "datos invalidos" generico.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex, WebRequest req) {
		Map<String, String> campos = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(e -> campos.put(e.getField(), e.getDefaultMessage()));

		Map<String, Object> cuerpo = cuerpoBase(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS",
				"Revisa los campos indicados", req);
		cuerpo.put("campos", campos);
		return ResponseEntity.badRequest().body(cuerpo);
	}

	/**
	 * Cualquier otra cosa -> 500.
	 *
	 * El mensaje tecnico NO se expone al cliente (podria filtrar detalles
	 * internos); se registra en el log y al cliente se le da el requestId para
	 * que pueda reportarlo.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> inesperado(Exception ex, WebRequest req) {
		// En un proyecto real esto iria a un logger, no a la consola.
		System.err.println("[ms-eventos] Error inesperado: " + ex);
		ex.printStackTrace();
		return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
				"Ocurrio un error inesperado. Reporta el requestId de esta respuesta.", req);
	}

	// -----------------------------------------------------------------

	private ResponseEntity<Map<String, Object>> respuesta(HttpStatus estado, String codigo,
			String mensaje, WebRequest req) {
		return ResponseEntity.status(estado).body(cuerpoBase(estado, codigo, mensaje, req));
	}

	private Map<String, Object> cuerpoBase(HttpStatus estado, String codigo,
			String mensaje, WebRequest req) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("error", codigo);
		cuerpo.put("mensaje", mensaje);
		cuerpo.put("estado", estado.value());
		cuerpo.put("ruta", rutaDe(req));
		cuerpo.put("requestId", req.getHeader("X-Request-Id"));
		cuerpo.put("fecha", Instant.now().toString());
		return cuerpo;
	}

	private String rutaDe(WebRequest req) {
		if (req instanceof ServletWebRequest swr) {
			return swr.getRequest().getRequestURI();
		}
		return null;
	}
}
