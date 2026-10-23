package com.teleticket.usuarios.config;

import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.teleticket.usuarios.entity.Rol;
import com.teleticket.usuarios.entity.Usuario;
import com.teleticket.usuarios.repository.UsuarioRepository;

@Component
@ConditionalOnProperty(name = "teleticket.usuarios.sembrar-datos", havingValue = "true")
public class DatosIniciales implements CommandLineRunner {

	private final UsuarioRepository repo;
	private final PasswordEncoder encoder;

	public DatosIniciales(UsuarioRepository repo, PasswordEncoder encoder) {
		this.repo = repo;
		this.encoder = encoder;
	}

	@Override
	public void run(String... args) {
		if (repo.count() > 0) {
			return;
		}
		String hash = encoder.encode("Teleticket123");
		sembrar(1, "70000001", "Ana Compradora", "ana@teleticket.test", hash, Rol.COMPRADOR);
		sembrar(2, "70000002", "Luis Comprador", "luis@teleticket.test", hash, Rol.COMPRADOR);
		sembrar(3, "70000003", "Rosa Compradora", "rosa@teleticket.test", hash, Rol.COMPRADOR);
		sembrar(4, "70000004", "Oscar Operador", "operador@teleticket.test", hash, Rol.OPERADOR);
		sembrar(5, "70000005", "Carla Controladora", "controlador@teleticket.test", hash, Rol.CONTROLADOR);
		sembrar(6, "70000006", "Mario Atencion", "atencion@teleticket.test", hash, Rol.ATENCION);
		System.out.println("[ms-usuarios] Sembrados 6 usuarios de prueba (contrasena: Teleticket123)");
	}

	private void sembrar(int n, String dni, String nombres, String email, String hash, Rol rol) {
		UUID id = UUID.fromString("00000000-0000-0000-0000-%012d".formatted(n));
		repo.save(Usuario.crear(id, dni, nombres, email, hash, rol));
	}
}
