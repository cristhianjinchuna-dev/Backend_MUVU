package com.example.desarrollo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PagoPublicidadRequestDTO {

    @NotNull(message = "El ID de la habitacion es obligatorio")
    @Positive(message = "El ID de la habitacion debe ser positivo")
    private Long habitacionId;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a cero")
    private Double monto;
}
