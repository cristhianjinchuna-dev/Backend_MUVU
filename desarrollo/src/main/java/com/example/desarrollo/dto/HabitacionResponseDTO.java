package com.example.desarrollo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class HabitacionResponseDTO {
    private Long id;
    private String direccion;
    private Double precio;
    private Double longitud;
    private Double latitud;
    private Boolean esDestacada;
    private Double distanciaKm;
    private Integer area;
    private Boolean oculta; // moderación: solo el dueño y el ADMIN ven habitaciones ocultas
}
