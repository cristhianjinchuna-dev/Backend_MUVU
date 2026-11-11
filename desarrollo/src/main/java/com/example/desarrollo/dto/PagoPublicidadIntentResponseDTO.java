package com.example.desarrollo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PagoPublicidadIntentResponseDTO {
    private String paymentIntentId;
    private String clientSecret;
}
