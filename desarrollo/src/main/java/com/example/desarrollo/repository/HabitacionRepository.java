package com.example.desarrollo.repository;

import com.example.desarrollo.model.Habitacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitacionRepository extends JpaRepository<Habitacion, Long> {

    Long countByArrendadorId(Long arrendadorId);

    Page<Habitacion> findByArrendadorId(Long arrendadorId, Pageable pageable);
}