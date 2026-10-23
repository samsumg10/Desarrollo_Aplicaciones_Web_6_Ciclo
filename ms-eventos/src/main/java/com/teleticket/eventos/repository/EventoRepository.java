package com.teleticket.eventos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleticket.eventos.entity.EstadoEvento;
import com.teleticket.eventos.entity.Evento;

/**
 * Acceso a la tabla evento.
 *
 * No hay que escribir la implementacion: Spring Data JPA la genera a partir
 * del nombre de cada metodo. 'findByEstadoOrderByInicioVentaAsc' se traduce
 * solo a "SELECT * FROM evento WHERE estado = ? ORDER BY inicio_venta".
 */
public interface EventoRepository extends JpaRepository<Evento, UUID> {

	/** Los eventos que el publico puede ver, los mas proximos primero. */
	List<Evento> findByEstadoOrderByInicioVentaAsc(EstadoEvento estado);

	/**
	 * Trae el evento con sus zonas en UNA sola consulta.
	 *
	 * Sin esto, JPA haria una consulta para el evento y otra para las zonas
	 * (el clasico problema N+1). 'LEFT JOIN FETCH' las junta.
	 */
	@Query("SELECT e FROM Evento e LEFT JOIN FETCH e.zonas WHERE e.id = :id")
	Optional<Evento> buscarConZonas(@Param("id") UUID id);

	boolean existsByNombreIgnoreCaseAndFecha(String nombre, java.time.Instant fecha);
}
