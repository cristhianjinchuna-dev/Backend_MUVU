package com.example.desarrollo.dto;

import com.example.desarrollo.model.Estado;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReservaResponseDTO {
    private Long id;
    private Estado estado;
    private LocalDate fecha_fin;
    private LocalDate fecha_inicio;
    private Long habitacionId;
    private Long estudianteId;
    // Datos de la otra parte (arrendador o estudiante). Solo llega cuando la reserva está CONFIRMADA
    private ContactoDTO contacto;
}
