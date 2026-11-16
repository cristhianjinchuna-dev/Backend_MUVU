package com.example.desarrollo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EstudianteResponseDTO {
    private Long id;
    private String nombre;
    private String correo;
    private boolean verificado;
    private String universidadNombre;
    private String fotoPerfil;
    // Solo lo ve el propio usuario o el ADMIN (el service lo oculta para los demás)
    private String telefono;
}
