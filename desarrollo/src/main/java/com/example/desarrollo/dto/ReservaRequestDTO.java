package com.example.desarrollo.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ReservaRequestDTO {

    @NotNull(message = "La fecha de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha de inicio no puede ser pasada")
    private LocalDate fecha_inicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    @Future(message = "La fecha de fin debe ser posterior a hoy")
    private LocalDate fecha_fin;

    @NotNull(message = "El ID de la habitación es obligatorio")
    @Positive(message = "El ID de la habitación debe ser positivo")
    private Long habitacionId;

    // Opcional: presentación del estudiante para el arrendador
    @Size(max = 500, message = "El mensaje no puede superar 500 caracteres")
    private String mensaje;
}