package org.citcuit.component;

import java.util.List;

import org.citcuit.bean.dto.UsuarioDto;
import org.citcuit.usecase.UsuarioDtoUseCase;
import org.springframework.stereotype.Component;

@Component
public class UsuarioDtoFallback implements UsuarioDtoUseCase {

	@Override
	public List<UsuarioDto> ListarUsuarios() {
        System.out.println("Espere por favor problemas de conexion ....!!! (Mal Clima)");
        return List.of(); 
	}


}