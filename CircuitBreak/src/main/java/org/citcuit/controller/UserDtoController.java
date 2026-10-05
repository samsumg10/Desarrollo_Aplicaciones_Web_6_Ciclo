package org.citcuit.controller;

import java.util.List;

import org.citcuit.bean.dto.UsuarioDto;
import org.citcuit.model.UsuarioDtoModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/api")
@RestController
public class UserDtoController {
	
	private final UsuarioDtoModel usuarioDtoModel;
	
	@GetMapping("/lista")
    public List<UsuarioDto> ListaUsuarios() {
        return usuarioDtoModel.ListarUsuarios();
    }
	
	@GetMapping("/api")
	public String Api(){
		return "Api ...!!!!";
	}


}
