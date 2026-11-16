package com.example.desarrollo.service;

import com.example.desarrollo.dto.EstudiantePerfilDTO;
import com.example.desarrollo.dto.EstudianteResponseDTO;
import com.example.desarrollo.dto.EstudianteUpdateRequestDTO;
import com.example.desarrollo.exceptions.ForbiddenException;
import com.example.desarrollo.exceptions.ResourceNotFoundException;
import com.example.desarrollo.model.Estudiante;
import com.example.desarrollo.model.Rol;
import com.example.desarrollo.model.Universidad;
import com.example.desarrollo.repository.CalificacionRepository;
import com.example.desarrollo.repository.EstudianteRepository;
import com.example.desarrollo.repository.UniversidadRepository;
import com.example.desarrollo.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstudianteServiceTest {

    @Mock private EstudianteRepository estudianteRepository;
    @Mock private UniversidadRepository universidadRepository;
    @Spy private ModelMapper modelMapper = new ModelMapper();
    @Mock private CalificacionRepository calificacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioService usuarioService;

    @InjectMocks private EstudianteService estudianteService;

    private Estudiante estudiante;

    @BeforeEach
    void setUp() {
        Universidad utec = new Universidad();
        utec.setId(1L);
        utec.setNombre("UTEC");

        estudiante = new Estudiante();
        estudiante.setId(3L);
        estudiante.setNombre("Ana");
        estudiante.setCorreo("ana@utec.edu.pe");
        estudiante.setRol(Rol.ESTUDIANTE);
        estudiante.setUniversidad(utec);
    }

    @Test
    void getById_existente_devuelveDTO() {
        when(estudianteRepository.findById(3L)).thenReturn(Optional.of(estudiante));

        EstudianteResponseDTO respuesta = estudianteService.getById(3L);

        assertEquals("Ana", respuesta.getNombre());
        assertEquals("UTEC", respuesta.getUniversidadNombre());
    }

    @Test
    void getById_inexistente_lanzaNotFound() {
        when(estudianteRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> estudianteService.getById(3L));
    }

    @Test
    void getAll_devuelvePagina() {
        when(estudianteRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(estudiante)));

        Page<EstudianteResponseDTO> pagina = estudianteService.getAll(PageRequest.of(0, 20));

        assertEquals(1, pagina.getTotalElements());
    }

    @Test
    void update_cambiaNombreYUniversidad() {
        Universidad pucp = new Universidad();
        pucp.setId(2L);
        pucp.setNombre("PUCP");
        when(estudianteRepository.findById(3L)).thenReturn(Optional.of(estudiante));
        when(universidadRepository.findById(2L)).thenReturn(Optional.of(pucp));
        when(estudianteRepository.save(estudiante)).thenReturn(estudiante);

        EstudianteResponseDTO respuesta = estudianteService.updateEstudiante(3L, new EstudianteUpdateRequestDTO("Ana María", 2L, null, null, null));

        verify(usuarioService).validarQueSoyYo(3L);
        assertEquals("Ana María", respuesta.getNombre());
        assertEquals("PUCP", respuesta.getUniversidadNombre());
    }

    @Test
    void update_deOtroUsuario_lanzaForbidden() {
        doThrow(new ForbiddenException("No")).when(usuarioService).validarQueSoyYo(3L);

        assertThrows(ForbiddenException.class,
                () -> estudianteService.updateEstudiante(3L, new EstudianteUpdateRequestDTO("Ana", 1L, null, null, null)));
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    void getPerfil_incluyePromedioYTotalDeCalificaciones() {
        when(estudianteRepository.findById(3L)).thenReturn(Optional.of(estudiante));
        when(calificacionRepository.findPromedioByAutorId(3L)).thenReturn(4.5);
        when(calificacionRepository.countByAutorId(3L)).thenReturn(2L);

        EstudiantePerfilDTO perfil = estudianteService.getPerfil(3L);

        assertEquals(4.5, perfil.getPuntuacionPromedio());
        assertEquals(2L, perfil.getTotalCalificaciones());
    }

    @Test
    void delete_existente_loBorra() {
        when(estudianteRepository.existsById(3L)).thenReturn(true);

        estudianteService.delete(3L);

        verify(estudianteRepository).deleteById(3L);
    }

    @Test
    void delete_inexistente_lanzaNotFound() {
        when(estudianteRepository.existsById(3L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> estudianteService.delete(3L));
    }
}
