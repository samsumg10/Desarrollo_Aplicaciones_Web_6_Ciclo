package com.teleticket.eventos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleticket.eventos.entity.EstadoEvento;
import com.teleticket.eventos.entity.Evento;

public interface EventoRepository extends JpaRepository<Evento, UUID> {

	List<Evento> findByEstadoOrderByInicioVentaAsc(EstadoEvento estado);

	@Query("SELECT e FROM Evento e LEFT JOIN FETCH e.zonas WHERE e.id = :id")
	Optional<Evento> buscarConZonas(@Param("id") UUID id);

	boolean existsByNombreIgnoreCaseAndFecha(String nombre, java.time.Instant fecha);
}
