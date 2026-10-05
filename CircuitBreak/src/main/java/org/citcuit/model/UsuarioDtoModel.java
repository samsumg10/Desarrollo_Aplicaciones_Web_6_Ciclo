package org.citcuit.model;

import java.util.List;

import org.citcuit.bean.dto.UsuarioDto;
import org.citcuit.usecase.UsuarioDtoUseCase;
import org.springframework.stereotype.Service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioDtoModel {

	private final UsuarioDtoUseCase usuarioDtoUseCase;

	@CircuitBreaker(name = "usuarioService", fallbackMethod = "fallbackUsuarios")
	public List<UsuarioDto> ListarUsuarios() {
		return usuarioDtoUseCase.ListarUsuarios();
	}

	public List<UsuarioDto> fallbackUsuarios(Throwable ex) {
	    System.out.println("FALLBACK EJECUTADO: " + ex.getClass().getSimpleName());
	    System.out.println("PROBLEMAS CON LA COMUNICACION POR FAVOR VOLVER A INTENTAR");
	    return List.of();
	}

}