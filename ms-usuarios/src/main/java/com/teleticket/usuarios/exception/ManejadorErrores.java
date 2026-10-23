package com.teleticket.usuarios.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

/** Convierte las excepciones en JSON con el mismo formato que ms-eventos. */
@RestControllerAdvice
public class ManejadorErrores {

	@ExceptionHandler(NoEncontradoException.class)
	public ResponseEntity<Map<String, Object>> noEncontrado(NoEncontradoException ex, WebRequest req) {
		return resp(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", ex.getMessage(), req);
	}

	@ExceptionHandler(ReglaNegocioException.class)
	public ResponseEntity<Map<String, Object>> regla(ReglaNegocioException ex, WebRequest req) {
		return resp(HttpStatus.CONFLICT, ex.getCodigo(), ex.getMessage(), req);
	}

	@ExceptionHandler(NoAutenticadoException.class)
	public ResponseEntity<Map<String, Object>> noAutenticado(NoAutenticadoException ex, WebRequest req) {
		return resp(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO", ex.getMessage(), req);
	}

	/**
	 * Dos registros simultaneos con el mismo DNI pasan la comprobacion previa a la vez;
	 * la restriccion UNIQUE de la base rechaza al segundo y se traduce aqui a 409.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, Object>> integridad(DataIntegrityViolationException ex, WebRequest req) {
		return resp(HttpStatus.CONFLICT, "DATOS_DUPLICADOS",
				"Ya existe un usuario con ese DNI o ese correo", req);
	}

	/** JSON mal formado o tipos incorrectos -> 400 (sin esto caeria en el 500 generico). */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, Object>> jsonInvalido(HttpMessageNotReadableException ex, WebRequest req) {
		return resp(HttpStatus.BAD_REQUEST, "JSON_INVALIDO",
				"El cuerpo de la peticion no es un JSON valido", req);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex, WebRequest req) {
		Map<String, String> campos = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(e -> campos.put(e.getField(), e.getDefaultMessage()));
		Map<String, Object> cuerpo = base(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", "Revisa los campos indicados", req);
		cuerpo.put("campos", campos);
		return ResponseEntity.badRequest().body(cuerpo);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> inesperado(Exception ex, WebRequest req) {
		System.err.println("[ms-usuarios] Error inesperado: " + ex);
		ex.printStackTrace();
		return resp(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
				"Ocurrio un error inesperado. Reporta el requestId de esta respuesta.", req);
	}

	private ResponseEntity<Map<String, Object>> resp(HttpStatus s, String codigo, String msg, WebRequest req) {
		return ResponseEntity.status(s).body(base(s, codigo, msg, req));
	}

	private Map<String, Object> base(HttpStatus s, String codigo, String msg, WebRequest req) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("error", codigo);
		m.put("mensaje", msg);
		m.put("estado", s.value());
		m.put("ruta", req instanceof ServletWebRequest w ? w.getRequest().getRequestURI() : null);
		m.put("requestId", req.getHeader("X-Request-Id"));
		m.put("fecha", Instant.now().toString());
		return m;
	}
}
