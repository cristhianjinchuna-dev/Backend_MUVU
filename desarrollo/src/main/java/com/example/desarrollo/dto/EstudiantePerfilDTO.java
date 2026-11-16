package com.example.desarrollo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EstudiantePerfilDTO {
    private Long id;
    private String nombre;
    private boolean verificado;
    private Long universidadId;
    private String universidadNombre;
    private Double puntuacionPromedio;
    private long totalCalificaciones;
    private String fotoPerfil;
    private String descripcion;
}
