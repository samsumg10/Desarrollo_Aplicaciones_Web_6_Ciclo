package com.teleticket.cola.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleticket.cola.entity.PaseCompra;

public interface PaseCompraRepository extends JpaRepository<PaseCompra, UUID> {

	Optional<PaseCompra> findByTurnoId(UUID turnoId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE pase_compra SET estado = 'USADO'
			 WHERE codigo = :codigo AND usuario_id = :usuarioId
			   AND estado = 'VIGENTE' AND expira_en > now()
			""", nativeQuery = true)
	int consumir(@Param("codigo") UUID codigo, @Param("usuarioId") UUID usuarioId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE pase_compra SET estado = 'USADO'
			 WHERE codigo = :codigo AND usuario_id = :usuarioId AND evento_id = :eventoId
			   AND estado = 'VIGENTE' AND expira_en > now()
			""", nativeQuery = true)
	int consumirDeEvento(@Param("codigo") UUID codigo, @Param("usuarioId") UUID usuarioId,
			@Param("eventoId") UUID eventoId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE pase_compra SET estado = 'VIGENTE'
			 WHERE codigo = :codigo AND usuario_id = :usuarioId
			   AND estado = 'USADO' AND expira_en > now()
			""", nativeQuery = true)
	int devolver(@Param("codigo") UUID codigo, @Param("usuarioId") UUID usuarioId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE pase_compra SET estado = 'VENCIDO'
			 WHERE estado = 'VIGENTE' AND expira_en <= now()
			""", nativeQuery = true)
	int vencerPasados();
}
