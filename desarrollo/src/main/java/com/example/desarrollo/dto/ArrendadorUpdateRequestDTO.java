package com.example.desarrollo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ArrendadorUpdateRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;

    @Size(max = 500, message = "La URL del DNI no puede superar 500 caracteres")
    private String dniFoto;

    // null = mantener la foto actual, "" = quitarla, URL = foto nueva
    @Size(max = 500, message = "La URL de la foto de perfil no puede superar 500 caracteres")
    @Pattern(regexp = "^$|^https?://\\S+$", message = "La foto de perfil debe ser una URL que empiece con http:// o https://")
    private String fotoPerfil;

    // null = mantener el celular actual, "" = quitarlo, 9 dígitos = celular nuevo
    @Pattern(regexp = "^$|^9\\d{8}$", message = "El celular debe tener 9 dígitos y empezar con 9")
    private String telefono;
}
