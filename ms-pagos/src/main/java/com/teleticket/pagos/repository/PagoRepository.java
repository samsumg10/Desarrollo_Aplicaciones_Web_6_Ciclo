package com.teleticket.pagos.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleticket.pagos.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, UUID> {

	Optional<Pago> findByIdempotencyKey(String idempotencyKey);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			INSERT INTO pago (id, orden_id, monto, estado, tarjeta_ultimos4, idempotency_key,
			                  intentos, creado_en, actualizado_en)
			VALUES (:id, :ordenId, :monto, 'PENDIENTE', :ultimos4, :clave, 1, now(), now())
			ON CONFLICT (idempotency_key) DO NOTHING
			""", nativeQuery = true)
	int iniciar(@Param("id") UUID id, @Param("ordenId") UUID ordenId, @Param("monto") BigDecimal monto,
			@Param("ultimos4") String ultimos4, @Param("clave") String clave);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE pago
			   SET estado = 'PENDIENTE', referencia_pasarela = NULL, motivo_rechazo = NULL,
			       tarjeta_ultimos4 = :ultimos4, intentos = intentos + 1, actualizado_en = now()
			 WHERE id = :id AND estado = 'RECHAZADO'
			""", nativeQuery = true)
	int reintentar(@Param("id") UUID id, @Param("ultimos4") String ultimos4);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE pago
			   SET estado = :estado, referencia_pasarela = :referencia, motivo_rechazo = :motivo,
			       actualizado_en = now()
			 WHERE id = :id AND estado = 'PENDIENTE'
			""", nativeQuery = true)
	int resolver(@Param("id") UUID id, @Param("estado") String estado,
			@Param("referencia") String referencia, @Param("motivo") String motivo);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			INSERT INTO reembolso (id, pago_id, monto, motivo, creado_en)
			VALUES (:id, :pagoId, :monto, :motivo, now())
			ON CONFLICT (pago_id) DO NOTHING
			""", nativeQuery = true)
	int registrarReembolso(@Param("id") UUID id, @Param("pagoId") UUID pagoId, @Param("monto") BigDecimal monto,
			@Param("motivo") String motivo);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE pago SET estado = 'REEMBOLSADO', actualizado_en = now()
			 WHERE id = :id AND estado = 'APROBADO'
			""", nativeQuery = true)
	int marcarReembolsado(@Param("id") UUID id);
}
