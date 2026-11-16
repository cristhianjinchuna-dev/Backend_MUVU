package com.example.desarrollo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// PATCH /habitaciones/{id}/visibilidad (solo ADMIN)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VisibilidadRequestDTO {

    @NotNull(message = "Indica si la habitación queda oculta (true) o visible (false)")
    private Boolean oculta;

    // Se incluye en el correo que recibe el arrendador
    @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
    private String motivo;
}
