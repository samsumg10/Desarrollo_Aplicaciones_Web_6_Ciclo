package org.api.usecase;

import java.util.List;

import org.api.bean.dto.UsuarioDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "usuario", url = UsuarioDtoUseCase.http )
public interface UsuarioDtoUseCase {

	String http = "https://jsonplaceholder.typicode.com";
	
	@GetMapping("/todos")
	public List<UsuarioDto> ListarUsuarios();

}
