package org.citcuit.usecase;

import java.util.List;

import org.citcuit.bean.dto.UsuarioDto;
import org.citcuit.component.UsuarioDtoFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
	    name = "usuario",
	    url = "https://jsonplaceholder.typicode.com1",
	    fallback = UsuarioDtoFallback.class
	)
public interface UsuarioDtoUseCase {

    @GetMapping("/todos")
    public List<UsuarioDto> ListarUsuarios();
}
    