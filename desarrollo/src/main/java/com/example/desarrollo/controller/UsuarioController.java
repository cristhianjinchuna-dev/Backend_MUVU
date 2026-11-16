package com.example.desarrollo.controller;

import com.example.desarrollo.dto.SuspensionRequestDTO;
import com.example.desarrollo.service.ModeracionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Moderación de cuentas (estudiantes y arrendadores)
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final ModeracionService moderacionService;

    @PreAuthorize("hasAuthority('ADMIN')")
    @PatchMapping("/{id}/suspension")
    public ResponseEntity<Void> cambiarSuspension(@PathVariable Long id, @Valid @RequestBody SuspensionRequestDTO dto) {
        moderacionService.cambiarSuspension(id, dto);
        return ResponseEntity.noContent().build();
    }
}
