package com.example.desarrollo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CalificacionResponseDTO {
    private Long id;
    private int puntuacion;
    private String descripcion;
    private Long autorId;
    private String autorNombre;
    private String autorFotoPerfil;
    private Long habitacionId;
    private Long reservaId;
}
