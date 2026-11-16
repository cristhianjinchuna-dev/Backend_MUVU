package com.example.desarrollo.controller;

import com.example.desarrollo.dto.HabitacionDetailDTO;
import com.example.desarrollo.dto.HabitacionRequestDTO;
import com.example.desarrollo.dto.HabitacionResponseDTO;
import com.example.desarrollo.dto.VisibilidadRequestDTO;
import com.example.desarrollo.service.HabitacionService;
import com.example.desarrollo.service.ModeracionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/habitaciones")
@RequiredArgsConstructor
public class HabitacionController {

    private static final String ORDEN_POR_DEFECTO = "recomendadas";

    private final HabitacionService habitacionService;
    private final ModeracionService moderacionService;

    // Habitaciones dentro de un radio (km) alrededor de una universidad
    @GetMapping("/cercanas")
    public ResponseEntity<Page<HabitacionResponseDTO>> getHabitacionesCercanas(
            @RequestParam @Positive(message = "El ID de la universidad debe ser positivo") Long universidadId,
            @RequestParam(defaultValue = "5.0")
            @Positive(message = "El radio debe ser mayor a cero")
            @DecimalMax(value = "50.0", message = "El radio no puede superar 50 km") Double radioKm,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
            @Max(value = 100, message = "El tamaño de página no puede superar 100") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(habitacionService.findCercanas(universidadId, radioKm, pageable));
    }

    @PreAuthorize("hasAuthority('ARRENDADOR')")
    @PostMapping
    public ResponseEntity<HabitacionResponseDTO> createHabitacion(
            @Valid @RequestBody HabitacionRequestDTO habitacionRequestDTO) {
        HabitacionResponseDTO savedHabitacion = habitacionService.save(habitacionRequestDTO);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedHabitacion.getId())
                .toUri();

        return ResponseEntity.created(location).body(savedHabitacion);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HabitacionDetailDTO> getHabitacionById(@PathVariable Long id) {
        return ResponseEntity.ok(habitacionService.findById(id));
    }

    // Listado paginado. Filtros opcionales: q (dirección), precioMin, precioMax y orden
    @GetMapping
    public ResponseEntity<Page<HabitacionResponseDTO>> getAllHabitaciones(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
            @Max(value = 100, message = "El tamaño de página no puede superar 100") int size,
            @RequestParam(required = false)
            @Size(max = 100, message = "La búsqueda no puede superar 100 caracteres") String q,
            @RequestParam(required = false)
            @PositiveOrZero(message = "El precio mínimo no puede ser negativo") Double precioMin,
            @RequestParam(required = false)
            @PositiveOrZero(message = "El precio máximo no puede ser negativo") Double precioMax,
            @RequestParam(defaultValue = ORDEN_POR_DEFECTO)
            @Pattern(regexp = "recomendadas|precio_asc|precio_desc|area_desc",
                    message = "El orden debe ser recomendadas, precio_asc, precio_desc o area_desc") String orden) {
        Pageable pageable = PageRequest.of(page, size);

        boolean sinFiltros = (q == null || q.isBlank()) && precioMin == null && precioMax == null
                && ORDEN_POR_DEFECTO.equals(orden);
        if (sinFiltros) {
            return ResponseEntity.ok(habitacionService.findAll(pageable));
        }
        return ResponseEntity.ok(habitacionService.buscar(q, precioMin, precioMax, orden, pageable));
    }

    @PreAuthorize("hasAuthority('ARRENDADOR')")
    @PutMapping("/{id}")
    public ResponseEntity<HabitacionResponseDTO> updateHabitacion(
            @PathVariable Long id, @Valid @RequestBody HabitacionRequestDTO dto) {
        return ResponseEntity.ok(habitacionService.update(id, dto));
    }

    // Moderación: el ADMIN oculta (o vuelve a publicar) una habitación inapropiada
    @PreAuthorize("hasAuthority('ADMIN')")
    @PatchMapping("/{id}/visibilidad")
    public ResponseEntity<Void> cambiarVisibilidad(@PathVariable Long id, @Valid @RequestBody VisibilidadRequestDTO dto) {
        moderacionService.cambiarVisibilidad(id, dto);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('ARRENDADOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHabitacion(@PathVariable Long id) {
        habitacionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}