package com.example.desarrollo.repository;

import com.example.desarrollo.model.Habitacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitacionRepository extends JpaRepository<Habitacion, Long> {

    Long countByArrendadorId(Long arrendadorId);

    Page<Habitacion> findByArrendadorId(Long arrendadorId, Pageable pageable);

    // Búsqueda del listado principal: dirección que contenga el texto + precio entre min y max
    // Solo habitaciones públicas: no ocultas por el ADMIN y con el arrendador sin suspender
    Page<Habitacion> findByDireccionContainingIgnoreCaseAndPrecioBetweenAndOcultaFalseAndArrendadorSuspendidoFalse(
            String direccion, double precioMin, double precioMax, Pageable pageable);

    Page<Habitacion> findByOcultaFalseAndArrendadorSuspendidoFalse(Pageable pageable);

    // Perfil público de un arrendador: sin las ocultas
    Page<Habitacion> findByArrendadorIdAndOcultaFalse(Long arrendadorId, Pageable pageable);
}