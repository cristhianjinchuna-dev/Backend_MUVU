package com.example.desarrollo.service;

import com.example.desarrollo.Events.NotificacionCorreoEvent;
import com.example.desarrollo.dto.SuspensionRequestDTO;
import com.example.desarrollo.dto.VisibilidadRequestDTO;
import com.example.desarrollo.exceptions.InvalidOperationException;
import com.example.desarrollo.model.Admin;
import com.example.desarrollo.model.Arrendador;
import com.example.desarrollo.model.Habitacion;
import com.example.desarrollo.model.Rol;
import com.example.desarrollo.repository.HabitacionRepository;
import com.example.desarrollo.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModeracionServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private HabitacionRepository habitacionRepository;
    @Mock private ApplicationEventPublisher publisher;

    @InjectMocks private ModeracionService moderacionService;

    private Arrendador arrendador() {
        Arrendador a = new Arrendador();
        a.setId(2L);
        a.setNombre("Rosa");
        a.setCorreo("rosa@muvu.com");
        a.setRol(Rol.ARRENDADOR);
        return a;
    }

    @Test
    void suspender_bloqueaLaCuentaYAvisaPorCorreo() {
        Arrendador rosa = arrendador();
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(rosa));

        moderacionService.cambiarSuspension(2L, new SuspensionRequestDTO(true, "Publicaciones falsas"));

        assertTrue(rosa.getSuspendido());
        assertFalse(rosa.isEnabled()); // Spring Security ya no la deja iniciar sesión
        verify(publisher).publishEvent(any(NotificacionCorreoEvent.class));
    }

    @Test
    void suspender_unaCuentaYaSuspendida_noHaceNada() {
        Arrendador rosa = arrendador();
        rosa.setSuspendido(true);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(rosa));

        moderacionService.cambiarSuspension(2L, new SuspensionRequestDTO(true, null));

        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void suspender_aUnAdmin_lanzaInvalidOperation() {
        Admin admin = new Admin();
        admin.setId(1L);
        admin.setRol(Rol.ADMIN);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(InvalidOperationException.class,
                () -> moderacionService.cambiarSuspension(1L, new SuspensionRequestDTO(true, null)));
        assertFalse(admin.getSuspendido());
    }

    @Test
    void ocultarHabitacion_dejaDeSerPublicaYAvisaAlArrendador() {
        Habitacion habitacion = new Habitacion();
        habitacion.setId(5L);
        habitacion.setDireccion("Av. Grau 100");
        habitacion.setArrendador(arrendador());
        when(habitacionRepository.findById(5L)).thenReturn(Optional.of(habitacion));

        moderacionService.cambiarVisibilidad(5L, new VisibilidadRequestDTO(true, "Fotos engañosas"));

        assertTrue(habitacion.getOculta());
        assertFalse(HabitacionService.esPublica(habitacion));
        verify(publisher).publishEvent(any(NotificacionCorreoEvent.class));
    }
}
