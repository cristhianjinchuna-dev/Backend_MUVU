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
public class ArrendadorResponseDTO {
    private Long id;
    private String nombre;
    private String correo;
    private Boolean verificado;
    private Double puntajePromedio;
    private Long totalCalificaciones;
    private Long cantidadHabitaciones;
    private String fotoPerfil;
    // Solo lo ve el propio usuario o el ADMIN (el service lo oculta para los demás)
    private String telefono;
}
