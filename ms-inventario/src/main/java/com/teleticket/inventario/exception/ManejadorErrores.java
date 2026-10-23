package com.teleticket.inventario.exception;

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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ManejadorErrores {

	@ExceptionHandler(NoEncontradoException.class)
	public ResponseEntity<Map<String, Object>> noEncontrado(NoEncontradoException ex, WebRequest req) {
		return respuesta(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", ex.getMessage(), req);
	}

	@ExceptionHandler(ReglaNegocioException.class)
	public ResponseEntity<Map<String, Object>> reglaNegocio(ReglaNegocioException ex, WebRequest req) {
		return respuesta(HttpStatus.CONFLICT, ex.getCodigo(), ex.getMessage(), req);
	}

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

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, Object>> jsonInvalido(HttpMessageNotReadableException ex, WebRequest req) {
		return respuesta(HttpStatus.BAD_REQUEST, "JSON_INVALIDO",
				"El cuerpo de la peticion no es un JSON valido", req);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<Map<String, Object>> parametroInvalido(MethodArgumentTypeMismatchException ex, WebRequest req) {
		return respuesta(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO",
				"El parametro '%s' no tiene un formato valido".formatted(ex.getName()), req);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Map<String, Object>> rutaInexistente(NoResourceFoundException ex, WebRequest req) {
		return respuesta(HttpStatus.NOT_FOUND, "RUTA_INEXISTENTE", "No existe esa ruta", req);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> inesperado(Exception ex, WebRequest req) {
		System.err.println("[ms-inventario] Error inesperado: " + ex);
		ex.printStackTrace();
		return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
				"Ocurrio un error inesperado. Reporta el requestId de esta respuesta.", req);
	}

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
		cuerpo.put("ruta", req instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : null);
		cuerpo.put("requestId", req.getHeader("X-Request-Id"));
		cuerpo.put("fecha", Instant.now().toString());
		return cuerpo;
	}
}
