package com.example.desarrollo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EstudianteUpdateRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    @Pattern(regexp = "^[\\p{L} .'-]+$", message = "El nombre solo puede contener letras, espacios, puntos, apóstrofes y guiones")
    private String nombre;

    @NotNull(message = "La universidad es obligatoria")
    @Positive(message = "El ID de la universidad debe ser positivo")
    private Long universidadId;

    // null = mantener la foto actual, "" = quitarla, URL = foto nueva
    @Size(max = 500, message = "La URL de la foto de perfil no puede superar 500 caracteres")
    @Pattern(regexp = "^$|^https?://\\S+$", message = "La foto de perfil debe ser una URL que empiece con http:// o https://")
    private String fotoPerfil;

    // null = mantener el celular actual, "" = quitarlo, 9 dígitos = celular nuevo
    @Pattern(regexp = "^$|^9\\d{8}$", message = "El celular debe tener 9 dígitos y empezar con 9")
    private String telefono;
}