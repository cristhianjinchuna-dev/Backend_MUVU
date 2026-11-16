package com.example.desarrollo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "reserva", indexes = {
        @Index(name = "idx_reserva_estudiante", columnList = "estudiante_id"),
        @Index(name = "idx_reserva_habitacion", columnList = "habitacion_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fecha_inicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fecha_fin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "habitacion_id", nullable = false)
    private Habitacion habitacion;

    // Mensaje opcional del estudiante al arrendador al enviar la solicitud
    @Column(length = 500)
    private String mensaje;

    public Reserva(LocalDate fecha_inicio, LocalDate fecha_fin, Estudiante estudiante, Habitacion habitacion) {
        this.fecha_inicio = fecha_inicio;
        this.fecha_fin = fecha_fin;
        this.estado = Estado.PENDIENTE;
        this.estudiante = estudiante;
        this.habitacion = habitacion;
    }

    public boolean seCruzaCon(LocalDate inicio, LocalDate fin) {
        return fecha_inicio.isBefore(fin) && inicio.isBefore(fecha_fin);
    }
}