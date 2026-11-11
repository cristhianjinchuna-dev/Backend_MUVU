package com.example.desarrollo.repository;

import com.example.desarrollo.model.PagoPublicidad;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PagoPublicidadRepository extends JpaRepository<PagoPublicidad, Long> {

    java.util.Optional<PagoPublicidad> findByStripePaymentIntentId(String stripePaymentIntentId);

    @EntityGraph(attributePaths = "habitacion")
    List<PagoPublicidad> findByFechaFinBefore(LocalDate fecha);

    List<PagoPublicidad> findByHabitacionArrendadorId(Long arrendadorId);
}
