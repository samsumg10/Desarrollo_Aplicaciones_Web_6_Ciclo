package com.teleticket.inventario.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleticket.inventario.entity.StockZona;

/**
 * Acceso a la tabla stock_zona.
 *
 * Aqui estan las dos consultas que importan de todo el servicio. Las dos son
 * UN SOLO SQL, sin leer antes: por eso no necesitan bloqueos manuales.
 */
public interface StockZonaRepository extends JpaRepository<StockZona, UUID> {

	/**
	 * Descuenta stock SOLO si alcanza. Es la defensa contra la sobreventa.
	 *
	 * Devuelve las filas afectadas: 1 = reservado, 0 = no habia stock suficiente
	 * (o la zona no existe).
	 *
	 * Por que es segura: la condicion 'disponible >= :cantidad' y la resta
	 * ocurren juntas, dentro de la base de datos. Si dos compradores piden las
	 * ultimas 2 entradas a la vez, PostgreSQL bloquea la fila para el primero;
	 * el segundo espera, vuelve a evaluar la condicion con el valor ya
	 * descontado, y no la cumple: afecta 0 filas. Con el esquema inseguro
	 * ("leo disponible, resto en Java, guardo") los dos habrian leido "2".
	 *
	 * 'flushAutomatically' vuelca antes los cambios pendientes de JPA, y
	 * 'clearAutomatically' vacia despues la cache de entidades, que quedo
	 * desactualizada porque este UPDATE no pasa por ella.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE stock_zona
			   SET disponible = disponible - :cantidad,
			       version    = version + 1
			 WHERE zona_id = :zonaId
			   AND disponible >= :cantidad
			""", nativeQuery = true)
	int descontar(@Param("zonaId") UUID zonaId, @Param("cantidad") int cantidad);

	/**
	 * Crea el stock de una zona, o no hace nada si ya existe.
	 *
	 * No se usa save(): con un id asignado a mano, save() hace un "merge" y
	 * SOBRESCRIBIRIA el stock existente con el aforo, devolviendo al
	 * inventario entradas que ya se vendieron. 'ON CONFLICT DO NOTHING' lo
	 * impide, incluso si el mismo evento llegara con un mensaje distinto.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			INSERT INTO stock_zona (zona_id, evento_id, total, disponible, version)
			VALUES (:zonaId, :eventoId, :aforo, :aforo, 0)
			ON CONFLICT (zona_id) DO NOTHING
			""", nativeQuery = true)
	int crearSiNoExiste(@Param("zonaId") UUID zonaId,
			@Param("eventoId") UUID eventoId,
			@Param("aforo") int aforo);
}
