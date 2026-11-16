package com.example.desarrollo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Solo para el listado del ADMIN (GET /arrendadores): incluye la foto del DNI para verificar al arrendador.
// No se usa en GET /arrendadores/{id} porque ese endpoint lo puede ver cualquier usuario logueado.
@Getter
@Setter
@NoArgsConstructor
public class ArrendadorAdminDTO extends ArrendadorResponseDTO {
    private String dniFoto;
}
