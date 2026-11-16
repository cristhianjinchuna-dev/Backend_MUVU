package com.example.desarrollo.service;

import lombok.extern.slf4j.Slf4j;
import com.example.desarrollo.dto.EstudianteRequestDTO;
import com.example.desarrollo.dto.EstudianteResponseDTO;
import com.example.desarrollo.exceptions.ConflictException;
import com.example.desarrollo.exceptions.ResourceNotFoundException;
import com.example.desarrollo.model.Estudiante;
import com.example.desarrollo.model.Universidad;
import com.example.desarrollo.repository.EstudianteRepository;
import com.example.desarrollo.repository.UniversidadRepository;
import com.example.desarrollo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.example.desarrollo.dto.EstudiantePerfilDTO;
import com.example.desarrollo.dto.EstudianteUpdateRequestDTO;
import com.example.desarrollo.repository.CalificacionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EstudianteService {
    private final EstudianteRepository estudianteRepository;
    private final UniversidadRepository universidadRepository;
    private final ModelMapper modelMapper;
    private final CalificacionRepository calificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    // PUT (Actualizar estudiante)
    public EstudianteResponseDTO getById(Long id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));
        return modelMapper.map(estudiante, EstudianteResponseDTO.class);
    }

    // GET (Obtener todos los estudiantes paginados)
    public Page<EstudianteResponseDTO> getAll(Pageable pageable) {
        return estudianteRepository.findAll(pageable)
                .map(e -> modelMapper.map(e, EstudianteResponseDTO.class));
    }

    public EstudianteResponseDTO updateEstudiante(Long id, EstudianteUpdateRequestDTO dto) {
        usuarioService.validarQueSoyYo(id);
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));

        Universidad universidad = universidadRepository.findById(dto.getUniversidadId())
                .orElseThrow(() -> new ResourceNotFoundException("Universidad no encontrado"));

        estudiante.setNombre(dto.getNombre());
        estudiante.setUniversidad(universidad);
        // null = mantener la foto actual, "" = quitarla, URL = foto nueva
        if (dto.getFotoPerfil() != null) {
            estudiante.setFotoPerfil(dto.getFotoPerfil().isBlank() ? null : dto.getFotoPerfil());
        }
        log.info("Estudiante {} actualizó su perfil", id);

        return modelMapper.map(estudianteRepository.save(estudiante), EstudianteResponseDTO.class);
    }

    // GET (Informacion relevante para Arrendador)
    public EstudiantePerfilDTO getPerfil(Long id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));

        EstudiantePerfilDTO perfil = modelMapper.map(estudiante, EstudiantePerfilDTO.class);
        perfil.setPuntuacionPromedio(calificacionRepository.findPromedioByAutorId(id));
        perfil.setTotalCalificaciones(calificacionRepository.countByAutorId(id));

        return perfil;
    }

    // DELETE (Eliminar estudiante)
    public void delete(Long id) {
        usuarioService.validarQueSoyYo(id);
        if (!estudianteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Estudiante no encontrado");
        }
        estudianteRepository.deleteById(id);
        log.info("Estudiante {} eliminado", id);
    }
}
