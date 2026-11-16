package com.example.desarrollo.service;

import com.example.desarrollo.Events.ActualizacionPromedioEvent;
import com.example.desarrollo.Events.NotificacionCorreoEvent;
import com.example.desarrollo.dto.CalificacionRequestDTO;
import com.example.desarrollo.dto.CalificacionResponseDTO;
import com.example.desarrollo.exceptions.DuplicateResourceException;
import com.example.desarrollo.exceptions.ForbiddenException;
import com.example.desarrollo.exceptions.ReservaInvalidStateException;
import com.example.desarrollo.exceptions.ResourceNotFoundException;
import com.example.desarrollo.model.*;
import com.example.desarrollo.repository.CalificacionRepository;
import com.example.desarrollo.repository.EstudianteRepository;
import com.example.desarrollo.repository.HabitacionRepository;
import com.example.desarrollo.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalificacionServiceTest {

    @Mock private CalificacionRepository calificacionRepository;
    @Spy private ModelMapper modelMapper = new ModelMapper();
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private ReservaRepository reservaRepository;
    @Mock private HabitacionRepository habitacionRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private ApplicationEventPublisher publisher;

    @InjectMocks private CalificacionService calificacionService;

    private Estudiante estudiante;
    private Habitacion habitacion;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        estudiante = new Estudiante();
        estudiante.setId(3L);
        estudiante.setNombre("Ana");
        estudiante.setRol(Rol.ESTUDIANTE);

        Arrendador arrendador = new Arrendador();
        arrendador.setId(2L);
        arrendador.setRol(Rol.ARRENDADOR);

        habitacion = new Habitacion();
        habitacion.setId(1L);
        habitacion.setArrendador(arrendador);

        reserva = new Reserva(LocalDate.now(), LocalDate.now().plusDays(5), estudiante, habitacion);
        reserva.setId(10L);
        reserva.setEstado(Estado.CONFIRMADO);
    }

    @Test
    void crear_conReservaConfirmada_guardaYActualizaPromedio() {
        prepararCreacion(3L, estudiante);
        when(calificacionRepository.existsByReservaIdAndAutorId(10L, 3L)).thenReturn(false);
        when(calificacionRepository.save(any(Calificacion.class))).thenAnswer(inv -> {
            Calificacion c = inv.getArgument(0);
            c.setId(5L);
            return c;
        });

        CalificacionResponseDTO respuesta = calificacionService.create(new CalificacionRequestDTO(5, "Excelente", 10L));

        assertEquals(5L, respuesta.getId());
        assertEquals(5, respuesta.getPuntuacion());
        assertEquals(1L, respuesta.getHabitacionId());
        verify(publisher).publishEvent(any(ActualizacionPromedioEvent.class));
    }

    @Test
    void crear_porOtroEstudiante_lanzaForbidden() {
        Estudiante otro = new Estudiante();
        otro.setId(99L);
        prepararCreacion(99L, otro);

        assertThrows(ForbiddenException.class,
                () -> calificacionService.create(new CalificacionRequestDTO(5, "x", 10L)));
        verify(calificacionRepository, never()).save(any());
    }

    @Test
    void crear_conReservaPendiente_lanzaEstadoInvalido() {
        reserva.setEstado(Estado.PENDIENTE);
        prepararCreacion(3L, estudiante);

        assertThrows(ReservaInvalidStateException.class,
                () -> calificacionService.create(new CalificacionRequestDTO(4, "x", 10L)));
    }

    @Test
    void crear_reservaYaCalificada_lanzaDuplicate() {
        prepararCreacion(3L, estudiante);
        when(calificacionRepository.existsByReservaIdAndAutorId(10L, 3L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> calificacionService.create(new CalificacionRequestDTO(4, "x", 10L)));
    }

    @Test
    void crear_conReservaInexistente_lanzaNotFound() {
        when(reservaRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> calificacionService.create(new CalificacionRequestDTO(4, "x", 10L)));
    }

    @Test
    void findById_inexistente_lanzaNotFound() {
        when(calificacionRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> calificacionService.findById(5L));
    }

    @Test
    void findById_devuelveCalificacion() {
        when(calificacionRepository.findById(5L)).thenReturn(Optional.of(calificacion()));

        assertEquals(1L, calificacionService.findById(5L).getHabitacionId());
    }

    @Test
    void findByHabitacion_devuelveSusCalificaciones() {
        when(habitacionRepository.existsById(1L)).thenReturn(true);
        when(calificacionRepository.findByReceptorId(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(calificacion())));

        assertEquals(1, calificacionService.findByHabitacion(1L, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void findByHabitacion_inexistente_lanzaNotFound() {
        when(habitacionRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> calificacionService.findByHabitacion(1L, PageRequest.of(0, 10)));
    }

    @Test
    void findByEstudiante_devuelveSusCalificaciones() {
        when(estudianteRepository.existsById(3L)).thenReturn(true);
        when(calificacionRepository.findByAutorId(eq(3L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(calificacion())));

        assertEquals(1, calificacionService.findByEstudiante(3L, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void findByEstudiante_inexistente_lanzaNotFound() {
        when(estudianteRepository.existsById(3L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> calificacionService.findByEstudiante(3L, PageRequest.of(0, 10)));
    }

    @Test
    void eliminar_validaAutorYActualizaPromedio() {
        Calificacion calificacion = calificacion();
        when(calificacionRepository.findById(5L)).thenReturn(Optional.of(calificacion));
        when(usuarioService.getIdUsuarioActual()).thenReturn(3L); // la borra su propio autor

        calificacionService.deleteById(5L, null);

        verify(usuarioService).validarQueSoyYo(3L);
        verify(calificacionRepository).delete(calificacion);
        verify(publisher).publishEvent(any(ActualizacionPromedioEvent.class));
        verify(publisher, never()).publishEvent(any(NotificacionCorreoEvent.class)); // no se avisa a sí mismo
    }

    @Test
    void eliminar_porElAdmin_avisaAlAutorPorCorreo() {
        Calificacion calificacion = calificacion();
        when(calificacionRepository.findById(5L)).thenReturn(Optional.of(calificacion));
        when(usuarioService.getIdUsuarioActual()).thenReturn(1L); // ADMIN (validarQueSoyYo lo deja pasar)

        calificacionService.deleteById(5L, "Lenguaje ofensivo");

        verify(calificacionRepository).delete(calificacion);
        verify(publisher).publishEvent(any(NotificacionCorreoEvent.class));
    }

    private void prepararCreacion(Long miId, Estudiante yo) {
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));
        when(usuarioService.getIdUsuarioActual()).thenReturn(miId);
        when(estudianteRepository.findById(miId)).thenReturn(Optional.of(yo));
    }

    private Calificacion calificacion() {
        Calificacion c = new Calificacion();
        c.setId(5L);
        c.setPuntuacion(5);
        c.setAutor(estudiante);
        c.setReceptor(habitacion);
        c.setReserva(reserva);
        return c;
    }
}
