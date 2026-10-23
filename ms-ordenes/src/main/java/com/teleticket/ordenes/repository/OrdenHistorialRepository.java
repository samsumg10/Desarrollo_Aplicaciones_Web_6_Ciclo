package com.teleticket.ordenes.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teleticket.ordenes.entity.OrdenHistorial;

public interface OrdenHistorialRepository extends JpaRepository<OrdenHistorial, Long> {

	List<OrdenHistorial> findByOrdenIdOrderByFechaAscIdAsc(UUID ordenId);
}
