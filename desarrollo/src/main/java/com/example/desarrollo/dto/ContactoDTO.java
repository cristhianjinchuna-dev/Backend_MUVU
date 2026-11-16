package com.example.desarrollo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Datos para que estudiante y arrendador se comuniquen una vez confirmada la reserva
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContactoDTO {
    private Long id;
    private String nombre;
    private String correo;
    private String telefono;
    private String fotoPerfil;
}
