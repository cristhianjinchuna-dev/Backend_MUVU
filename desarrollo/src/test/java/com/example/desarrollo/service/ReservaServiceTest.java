package com.example.desarrollo.service;

import com.example.desarrollo.Events.NotificacionCorreoEvent;
import com.example.desarrollo.dto.ReservaRequestDTO;
import com.example.desarrollo.dto.ReservaResponseDTO;
import com.example.desarrollo.exceptions.ConflictException;
import com.example.desarrollo.exceptions.ForbiddenException;
import com.example.desarrollo.exceptions.InvalidOperationException;
import com.example.desarrollo.exceptions.ReservaInvalidStateException;
import com.example.desarrollo.model.*;
import com.example.desarrollo.repository.EstudianteRepository;
import com.example.desarrollo.repository.HabitacionRepository;
import com.example.desarrollo.repository.ReservaRepository;
import com.example.desarrollo.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private HabitacionRepository habitacionRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private ApplicationEventPublisher publisher;

    @InjectMocks private ReservaService reservaService;

    private final LocalDate hoy = LocalDate.now();
    private Estudiante estudiante;
    private Habitacion habitacion;

    @BeforeEach
    void setUp() {
        estudiante = new Estudiante();
        estudiante.setId(3L);
        estudiante.setNombre("Ana");
        estudiante.setCorreo("ana@utec.edu.pe");

        Arrendador arrendador = new Arrendador();
        arrendador.setId(2L);

        habitacion = new Habitacion();
        habitacion.setId(1L);
        habitacion.setDireccion("Av. Pedro de Osma 120");
        habitacion.setArrendador(arrendador);
    }

    @Test
    void crearReserva_conFechasLibres_quedaPendiente() {
        prepararCreacion();
        when(reservaRepository.findByHabitacionIdAndEstadoIn(eq(1L), anyCollection())).thenReturn(List.of());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservaResponseDTO respuesta = reservaService.createReserva(solicitud(hoy.plusDays(1), hoy.plusDays(5)));

        assertEquals(Estado.PENDIENTE, respuesta.getEstado());
        assertEquals(3L, respuesta.getEstudianteId());
        assertEquals(1L, respuesta.getHabitacionId());
        verify(publisher, times(2)).publishEvent(any(NotificacionCorreoEvent.class)); // estudiante y arrendador
    }

    @Test
    void crearReserva_conFechasOcupadas_lanzaConflict() {
        prepararCreacion();
        Reserva existente = new Reserva(hoy.plusDays(3), hoy.plusDays(10), estudiante, habitacion);
        when(reservaRepository.findByHabitacionIdAndEstadoIn(eq(1L), anyCollection())).thenReturn(List.of(existente));

        assertThrows(ConflictException.class,
                () -> reservaService.createReserva(solicitud(hoy.plusDays(1), hoy.plusDays(5))));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void crearReserva_conFinAntesDelInicio_lanzaIllegalArgument() {
        prepararCreacion();

        assertThrows(InvalidOperationException.class,
                () -> reservaService.createReserva(solicitud(hoy.plusDays(5), hoy.plusDays(1))));
    }

    @Test
    void confirmarReserva_porQuienNoEsDueno_lanzaAccessDenied() {
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reservaPendiente()));
        when(usuarioService.getIdUsuarioActual()).thenReturn(99L);

        assertThrows(ForbiddenException.class, () -> reservaService.confirmReserva(10L));
    }

    @Test
    void confirmarReserva_porElDueno_confirmaYNotificaAlEstudiante() {
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reservaPendiente()));
        when(usuarioService.getIdUsuarioActual()).thenReturn(2L);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservaResponseDTO respuesta = reservaService.confirmReserva(10L);

        assertEquals(Estado.CONFIRMADO, respuesta.getEstado());
        // El arrendador recibe en la respuesta el contacto del estudiante
        assertNotNull(respuesta.getContacto());
        assertEquals("ana@utec.edu.pe", respuesta.getContacto().getCorreo());
        verify(publisher, times(2)).publishEvent(any(NotificacionCorreoEvent.class)); // estudiante y arrendador
    }

    @Test
    void cancelarReserva_porElEstudiante_quedaCancelada() {
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reservaPendiente()));
        when(usuarioService.getIdUsuarioActual()).thenReturn(3L);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservaResponseDTO respuesta = reservaService.cancelReserva(10L);
        assertEquals(Estado.CANCELADO, respuesta.getEstado());
        assertNull(respuesta.getContacto()); // sin confirmar no se comparten datos
        verify(publisher).publishEvent(any(NotificacionCorreoEvent.class));
    }

    @Test
    void cancelarReserva_porOtroEstudiante_lanzaForbidden() {
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reservaPendiente()));
        when(usuarioService.getIdUsuarioActual()).thenReturn(99L);

        assertThrows(ForbiddenException.class, () -> reservaService.cancelReserva(10L));
    }

    @Test
    void cancelarReserva_yaConfirmada_lanzaEstadoInvalido() {
        Reserva reserva = reservaPendiente();
        reserva.setEstado(Estado.CONFIRMADO);
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));
        when(usuarioService.getIdUsuarioActual()).thenReturn(3L);

        assertThrows(ReservaInvalidStateException.class, () -> reservaService.cancelReserva(10L));
    }

    @Test
    void confirmarReserva_yaCancelada_lanzaEstadoInvalido() {
        Reserva reserva = reservaPendiente();
        reserva.setEstado(Estado.CANCELADO);
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));
        when(usuarioService.getIdUsuarioActual()).thenReturn(2L);

        assertThrows(ReservaInvalidStateException.class, () -> reservaService.confirmReserva(10L));
    }

    @Test
    void findById_porElEstudiante_devuelveLaReserva() {
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reservaPendiente()));
        when(usuarioService.getIdUsuarioActual()).thenReturn(3L);

        assertEquals(10L, reservaService.findById(10L).getId());
    }

    @Test
    void findById_porUnTercero_lanzaForbidden() {
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reservaPendiente()));
        when(usuarioService.getIdUsuarioActual()).thenReturn(99L);

        assertThrows(ForbiddenException.class, () -> reservaService.findById(10L));
    }

    @Test
    void findMisReservas_estudiante_veSoloLasSuyas() {
        estudiante.setRol(Rol.ESTUDIANTE);
        when(usuarioService.getIdUsuarioActual()).thenReturn(3L);
        when(usuarioRepository.findById(3L)).thenReturn(Optional.<Usuario>of(estudiante));
        when(reservaRepository.findByEstudianteId(eq(3L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(reservaPendiente())));

        assertEquals(1, reservaService.findMisReservas(PageRequest.of(0, 10)).getTotalElements());
        verify(reservaRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void findMisReservas_arrendador_veLasDeSusHabitaciones() {
        Arrendador arrendador = habitacion.getArrendador();
        arrendador.setRol(Rol.ARRENDADOR);
        when(usuarioService.getIdUsuarioActual()).thenReturn(2L);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.<Usuario>of(arrendador));
        when(reservaRepository.findByHabitacionArrendadorId(eq(2L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(reservaPendiente())));

        assertEquals(1, reservaService.findMisReservas(PageRequest.of(0, 10)).getTotalElements());
    }

    private void prepararCreacion() {
        when(usuarioService.getIdUsuarioActual()).thenReturn(3L);
        when(estudianteRepository.findById(3L)).thenReturn(Optional.of(estudiante));
        when(habitacionRepository.findById(1L)).thenReturn(Optional.of(habitacion));
    }

    private ReservaRequestDTO solicitud(LocalDate inicio, LocalDate fin) {
        return new ReservaRequestDTO(inicio, fin, 1L);
    }

    private Reserva reservaPendiente() {
        Reserva reserva = new Reserva(hoy.plusDays(1), hoy.plusDays(5), estudiante, habitacion);
        reserva.setId(10L);
        return reserva;
    }
}
