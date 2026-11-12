package com.example.desarrollo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HabitacionDetailDTO {
    private Long id;
    private String direccion;
    private Double latitud;
    private Double longitud;
    private double precio;
    private Integer area;
    private List<ImagenResponseDTO> imagenes;
}
