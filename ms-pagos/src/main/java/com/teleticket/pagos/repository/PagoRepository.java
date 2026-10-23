package com.teleticket.pagos.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleticket.pagos.entity.Pago;

/**
 * Acceso a la tabla pago.
 *
 * Las consultas que escriben comparten la idea que protege el stock y las
 * reservas: la comprobacion y el cambio van en UN SOLO paso de la base de datos,
 * asi que no hay hueco entre "mirar" y "cambiar" por el que se cuele otra peticion.
 * Todas devuelven cuantas filas cambiaron: 1 = lo logre yo, 0 = otra peticion se adelanto.
 */
public interface PagoRepository extends JpaRepository<Pago, UUID> {

	Optional<Pago> findByIdempotencyKey(String idempotencyKey);

	/**
	 * Reserva la clave de idempotencia creando el pago en PENDIENTE.
	 *
	 * Devuelve 1 si es la primera vez que se cobra esta orden y 0 si ya habia un pago.
	 * Insertar y comprobar es un solo paso: con "buscar y luego insertar", dos cobros
	 * simultaneos de la misma orden pasarian los dos la busqueda. Con UNIQUE e
	 * ON CONFLICT, solo uno inserta.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			INSERT INTO pago (id, orden_id, monto, estado, tarjeta_ultimos4, idempotency_key,
			                  intentos, creado_en, actualizado_en)
			VALUES (:id, :ordenId, :monto, 'PENDIENTE', :ultimos4, :clave, 1, now(), now())
			ON CONFLICT (idempotency_key) DO NOTHING
			""", nativeQuery = true)
	int iniciar(@Param("id") UUID id, @Param("ordenId") UUID ordenId, @Param("monto") BigDecimal monto,
			@Param("ultimos4") String ultimos4, @Param("clave") String clave);

	/**
	 * Un nuevo intento tras un rechazo: la misma fila vuelve a PENDIENTE.
	 *
	 * Solo funciona si esta RECHAZADO. Si dos reintentos llegan a la vez, uno obtiene 1
	 * y el otro 0, y el segundo no cobra.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE pago
			   SET estado = 'PENDIENTE', referencia_pasarela = NULL, motivo_rechazo = NULL,
			       tarjeta_ultimos4 = :ultimos4, intentos = intentos + 1, actualizado_en = now()
			 WHERE id = :id AND estado = 'RECHAZADO'
			""", nativeQuery = true)
	int reintentar(@Param("id") UUID id, @Param("ultimos4") String ultimos4);

	/** Anota lo que respondio la pasarela. Solo si el pago sigue PENDIENTE. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE pago
			   SET estado = :estado, referencia_pasarela = :referencia, motivo_rechazo = :motivo,
			       actualizado_en = now()
			 WHERE id = :id AND estado = 'PENDIENTE'
			""", nativeQuery = true)
	int resolver(@Param("id") UUID id, @Param("estado") String estado,
			@Param("referencia") String referencia, @Param("motivo") String motivo);

	/**
	 * Registra la devolucion del pago (fase 11). Devuelve 1 la primera vez y 0 si ya estaba devuelto:
	 * UNIQUE(pago_id) deja pasar a una sola peticion, igual que idempotency_key en el cobro.
	 * Hay que llamarlo dentro de una transaccion, junto con {@link #marcarReembolsado}.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			INSERT INTO reembolso (id, pago_id, monto, motivo, creado_en)
			VALUES (:id, :pagoId, :monto, :motivo, now())
			ON CONFLICT (pago_id) DO NOTHING
			""", nativeQuery = true)
	int registrarReembolso(@Param("id") UUID id, @Param("pagoId") UUID pagoId, @Param("monto") BigDecimal monto,
			@Param("motivo") String motivo);

	/** APROBADO -> REEMBOLSADO. Solo si sigue APROBADO: un pago ya reembolsado (o nunca cobrado) no cambia. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE pago SET estado = 'REEMBOLSADO', actualizado_en = now()
			 WHERE id = :id AND estado = 'APROBADO'
			""", nativeQuery = true)
	int marcarReembolsado(@Param("id") UUID id);
}
