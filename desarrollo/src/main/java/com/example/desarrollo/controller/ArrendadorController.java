package com.example.desarrollo.controller;

import com.example.desarrollo.dto.ArrendadorResponseDTO;
import com.example.desarrollo.dto.ArrendadorUpdateRequestDTO;
import com.example.desarrollo.dto.HabitacionResponseDTO;
import com.example.desarrollo.service.ArrendadorService;
import com.example.desarrollo.service.HabitacionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/arrendadores")
@RequiredArgsConstructor
public class ArrendadorController {
    private final ArrendadorService arrendadorService;
    private final HabitacionService habitacionService;

    // Lista de arrendadores para el panel del administrador (paginada)
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<ArrendadorResponseDTO>> getAll(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(arrendadorService.getAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArrendadorResponseDTO> getArrendadorById(@PathVariable Long id) {
        return ResponseEntity.ok(arrendadorService.findById(id));
    }

    // Habitaciones publicadas por un arrendador (panel del arrendador)
    @GetMapping("/{id}/habitaciones")
    public ResponseEntity<Page<HabitacionResponseDTO>> getHabitacionesDelArrendador(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
            @Max(value = 100, message = "El tamaño de página no puede superar 100") int size) {
        return ResponseEntity.ok(habitacionService.findByArrendador(id, PageRequest.of(page, size)));
    }

    @PreAuthorize("hasAuthority('ARRENDADOR')")
    @PutMapping("/{id}")
    public ResponseEntity<ArrendadorResponseDTO> updateArrendador(
            @PathVariable Long id, @Valid @RequestBody ArrendadorUpdateRequestDTO dto) {
        return ResponseEntity.ok(arrendadorService.update(id, dto));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PatchMapping("/{id}/verificar")
    public ResponseEntity<ArrendadorResponseDTO> verificarArrendador(@PathVariable Long id) {
        return ResponseEntity.ok(arrendadorService.verificar(id));
    }

    @PreAuthorize("hasAnyAuthority('ARRENDADOR', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArrendador(@PathVariable Long id) {
        arrendadorService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}