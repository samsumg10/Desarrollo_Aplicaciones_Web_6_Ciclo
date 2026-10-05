package org.api.bean.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioDto implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private Integer userId;
	private Integer id;
	private String title;
	private String completed;

}
