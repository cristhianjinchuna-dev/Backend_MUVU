package com.example.desarrollo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CrearPagoPublicidadIntentRequestDTO {

    @NotNull
    @Positive
    private Long habitacionId;

    @NotNull
    @Positive
    private Double monto;
}
