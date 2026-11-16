package com.example.desarrollo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// PATCH /usuarios/{id}/suspension (solo ADMIN)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SuspensionRequestDTO {

    @NotNull(message = "Indica si la cuenta queda suspendida (true) o activa (false)")
    private Boolean suspendido;

    // Se incluye en el correo que recibe el usuario
    @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
    private String motivo;
}
