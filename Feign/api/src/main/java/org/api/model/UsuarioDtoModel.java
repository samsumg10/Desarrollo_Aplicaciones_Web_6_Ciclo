package org.api.model;

import java.util.List;

import org.api.bean.dto.UsuarioDto;
import org.api.usecase.UsuarioDtoUseCase;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UsuarioDtoModel{
	
	private final UsuarioDtoUseCase usuarioDtoUseCase;

	public List<UsuarioDto> ListarUsuarios() {
		return usuarioDtoUseCase.ListarUsuarios();
	}

}
