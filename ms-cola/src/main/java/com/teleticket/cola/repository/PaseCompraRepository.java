package com.teleticket.cola.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleticket.cola.entity.PaseCompra;

/**
 * Acceso a la tabla pase_compra.
 */
public interface PaseCompraRepository extends JpaRepository<PaseCompra, UUID> {

	Optional<PaseCompra> findByTurnoId(UUID turnoId);

	/**
	 * Consume el pase: UN pase, UNA compra (PRB-03).
	 *
	 * Devuelve 1 si lo consumio y 0 si el pase no existe, es de otra persona,
	 * ya se uso o ya vencio: todo en un solo UPDATE condicional. Si dos
	 * peticiones consumen el mismo pase a la vez, la base serializa los UPDATE y
	 * solo el primero ve 'VIGENTE'; el segundo afecta 0 filas.
	 *
	 * La hora se compara con now() de la base de datos, no con la del servicio:
	 * con varias replicas, relojes distintos podrian discrepar en el limite.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE pase_compra SET estado = 'USADO'
			 WHERE codigo = :codigo AND usuario_id = :usuarioId
			   AND estado = 'VIGENTE' AND expira_en > now()
			""", nativeQuery = true)
	int consumir(@Param("codigo") UUID codigo, @Param("usuarioId") UUID usuarioId);

	/**
	 * Lo mismo que consumir, pero exigiendo ademas que el pase sea de ESE evento.
	 * Se comprueba dentro del UPDATE: si se leyera el pase antes para comparar, entre
	 * la lectura y el consumo otro proceso podria usarlo.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE pase_compra SET estado = 'USADO'
			 WHERE codigo = :codigo AND usuario_id = :usuarioId AND evento_id = :eventoId
			   AND estado = 'VIGENTE' AND expira_en > now()
			""", nativeQuery = true)
	int consumirDeEvento(@Param("codigo") UUID codigo, @Param("usuarioId") UUID usuarioId,
			@Param("eventoId") UUID eventoId);

	/**
	 * Devuelve un pase USADO a VIGENTE: la compensacion de ms-ordenes cuando consumio
	 * el pase pero no pudo crear la orden (sin stock, servicio caido...).
	 *
	 * Solo si sigue sin vencer: un pase que ya paso su hora no resucita. Devuelve 1 si
	 * lo devolvio y 0 en cualquier otro caso.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE pase_compra SET estado = 'VIGENTE'
			 WHERE codigo = :codigo AND usuario_id = :usuarioId
			   AND estado = 'USADO' AND expira_en > now()
			""", nativeQuery = true)
	int devolver(@Param("codigo") UUID codigo, @Param("usuarioId") UUID usuarioId);

	/** Marca VENCIDO a todo pase vigente cuya hora ya paso. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE pase_compra SET estado = 'VENCIDO'
			 WHERE estado = 'VIGENTE' AND expira_en <= now()
			""", nativeQuery = true)
	int vencerPasados();
}
