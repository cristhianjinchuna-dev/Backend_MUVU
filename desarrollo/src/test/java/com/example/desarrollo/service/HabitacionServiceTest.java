package com.example.desarrollo.service;

import com.example.desarrollo.Events.ActualizacionHabitacionesEvent;
import com.example.desarrollo.dto.HabitacionDetailDTO;
import com.example.desarrollo.dto.HabitacionRequestDTO;
import com.example.desarrollo.dto.HabitacionResponseDTO;
import com.example.desarrollo.exceptions.ForbiddenException;
import com.example.desarrollo.exceptions.ResourceNotFoundException;
import com.example.desarrollo.model.*;
import com.example.desarrollo.repository.ArrendadorRepository;
import com.example.desarrollo.repository.HabitacionRepository;
import com.example.desarrollo.repository.ImagenRepository;
import com.example.desarrollo.repository.UniversidadRepository;
import com.google.maps.model.LatLng;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HabitacionServiceTest {

    @Mock private GoogleMapsService googleMapsService;
    @Mock private HabitacionRepository habitacionRepository;
    @Mock private ImagenRepository imagenRepository;
    @Spy private ModelMapper modelMapper = new ModelMapper();
    @Mock private ArrendadorRepository arrendadorRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private ApplicationEventPublisher publisher;
    @Mock private UniversidadRepository universidadRepository;

    @InjectMocks private HabitacionService habitacionService;

    private Arrendador arrendador;

    @BeforeEach
    void setUp() {
        arrendador = new Arrendador();
        arrendador.setId(2L);
        arrendador.setRol(Rol.ARRENDADOR);
        arrendador.setVerificado(true);
    }

    @Test
    void findCercanas_filtraPorRadioYPoneDestacadasPrimero() {
        Universidad utec = new Universidad();
        utec.setId(1L);
        utec.setLatitud(-12.1353);
        utec.setLongitud(-77.0220);
        when(universidadRepository.findById(1L)).thenReturn(Optional.of(utec));
        when(habitacionRepository.findAll()).thenReturn(List.of(
                habitacion(1L, -12.1360, -77.0230, false),
                habitacion(2L, -12.1400, -77.0250, true),
                habitacion(3L, -12.0500, -77.0500, true),
                habitacion(4L, null, null, false)));

        Page<HabitacionResponseDTO> pagina = habitacionService.findCercanas(1L, 3.0, PageRequest.of(0, 10));

        assertEquals(2, pagina.getTotalElements());
        assertEquals(2L, pagina.getContent().get(0).getId());
        assertEquals(1L, pagina.getContent().get(1).getId());
        assertTrue(pagina.getContent().get(1).getDistanciaKm() < 1.0);
    }

    @Test
    void findCercanas_paginaFueraDeRango_devuelveVacia() {
        Universidad utec = new Universidad();
        utec.setLatitud(-12.1353);
        utec.setLongitud(-77.0220);
        when(universidadRepository.findById(1L)).thenReturn(Optional.of(utec));
        when(habitacionRepository.findAll()).thenReturn(List.of(habitacion(1L, -12.1360, -77.0230, false)));

        Page<HabitacionResponseDTO> pagina = habitacionService.findCercanas(1L, 3.0, PageRequest.of(5, 10));

        assertTrue(pagina.getContent().isEmpty());
        assertEquals(1, pagina.getTotalElements());
    }

    @Test
    void findCercanas_universidadInexistente_lanzaNotFound() {
        when(universidadRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> habitacionService.findCercanas(9L, 3.0, PageRequest.of(0, 10)));
    }

    @Test
    void save_arrendadorVerificado_geolocalizaYPublicaEvento() {
        when(usuarioService.getIdUsuarioActual()).thenReturn(2L);
        when(arrendadorRepository.findById(2L)).thenReturn(Optional.of(arrendador));
        when(googleMapsService.obtenerCoordenadas("Av. Grau 100")).thenReturn(new LatLng(-12.1, -77.0));
        when(habitacionRepository.save(any(Habitacion.class))).thenAnswer(inv -> {
            Habitacion h = inv.getArgument(0);
            h.setId(1L);
            return h;
        });

        HabitacionResponseDTO respuesta = habitacionService.save(new HabitacionRequestDTO("Av. Grau 100", 800.0, 15));

        assertEquals(1L, respuesta.getId());
        assertEquals(-12.1, respuesta.getLatitud());
        verify(publisher).publishEvent(any(ActualizacionHabitacionesEvent.class));
    }

    @Test
    void save_arrendadorNoVerificado_lanzaForbidden() {
        arrendador.setVerificado(false);
        when(usuarioService.getIdUsuarioActual()).thenReturn(2L);
        when(arrendadorRepository.findById(2L)).thenReturn(Optional.of(arrendador));

        assertThrows(ForbiddenException.class,
                () -> habitacionService.save(new HabitacionRequestDTO("Av. Grau 100", 800.0, 15)));
        verify(habitacionRepository, never()).save(any());
    }

    @Test
    void findById_devuelveDetalleConImagenes() {
        Habitacion h = habitacion(1L, -12.1, -77.0, false);
        Imagen imagen = new Imagen();
        imagen.setId(7L);
        imagen.setUrl("https://muvu.com/1.jpg");
        h.agregarImagen(imagen);
        when(habitacionRepository.findById(1L)).thenReturn(Optional.of(h));

        HabitacionDetailDTO detalle = habitacionService.findById(1L);

        assertEquals(1, detalle.getImagenes().size());
        assertEquals("https://muvu.com/1.jpg", detalle.getImagenes().get(0).getUrl());
    }

    @Test
    void findById_inexistente_lanzaNotFound() {
        when(habitacionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> habitacionService.findById(1L));
    }

    @Test
    void findAll_ordenaDestacadasPrimero() {
        when(habitacionRepository.findByOcultaFalseAndArrendadorSuspendidoFalse(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(habitacion(1L, -12.1, -77.0, true))));

        Page<HabitacionResponseDTO> pagina = habitacionService.findAll(PageRequest.of(0, 10));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(habitacionRepository).findByOcultaFalseAndArrendadorSuspendidoFalse(captor.capture());
        assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("esDestacada").getDirection());
        assertEquals(1, pagina.getContent().size());
    }

    @Test
    void update_conNuevaDireccion_recalculaCoordenadas() {
        Habitacion h = habitacion(1L, -12.1, -77.0, false);
        when(habitacionRepository.findById(1L)).thenReturn(Optional.of(h));
        when(usuarioService.getIdUsuarioActual()).thenReturn(2L);
        when(googleMapsService.obtenerCoordenadas("Av. Nueva 200")).thenReturn(new LatLng(-12.2, -77.1));

        HabitacionResponseDTO respuesta = habitacionService.update(1L, new HabitacionRequestDTO("Av. Nueva 200", 900.0, 20));

        assertEquals(-12.2, respuesta.getLatitud());
        assertEquals(900.0, respuesta.getPrecio());
    }

    @Test
    void update_mismaDireccion_noConsultaGoogleMaps() {
        Habitacion h = habitacion(1L, -12.1, -77.0, false);
        when(habitacionRepository.findById(1L)).thenReturn(Optional.of(h));
        when(usuarioService.getIdUsuarioActual()).thenReturn(2L);

        habitacionService.update(1L, new HabitacionRequestDTO("Calle 1", 900.0, 20));

        verify(googleMapsService, never()).obtenerCoordenadas(anyString());
    }

    @Test
    void deleteById_porOtroArrendador_lanzaForbidden() {
        when(habitacionRepository.findById(1L)).thenReturn(Optional.of(habitacion(1L, -12.1, -77.0, false)));
        when(usuarioService.getIdUsuarioActual()).thenReturn(99L);

        assertThrows(ForbiddenException.class, () -> habitacionService.deleteById(1L));
        verify(habitacionRepository, never()).delete(any());
    }

    @Test
    void deleteById_porDueno_eliminaYPublicaEvento() {
        Habitacion h = habitacion(1L, -12.1, -77.0, false);
        when(habitacionRepository.findById(1L)).thenReturn(Optional.of(h));
        when(usuarioService.getIdUsuarioActual()).thenReturn(2L);

        habitacionService.deleteById(1L);

        verify(habitacionRepository).delete(h);
        verify(publisher).publishEvent(any(ActualizacionHabitacionesEvent.class));
    }

    private Habitacion habitacion(Long id, Double latitud, Double longitud, boolean destacada) {
        Habitacion h = new Habitacion();
        h.setId(id);
        h.setDireccion("Calle 1");
        h.setPrecio(700.0);
        h.setArea(12);
        h.setLatitud(latitud);
        h.setLongitud(longitud);
        h.setEsDestacada(destacada);
        h.setArrendador(arrendador);
        return h;
    }
}
