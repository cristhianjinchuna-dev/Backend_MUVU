package com.example.desarrollo.service;

import lombok.extern.slf4j.Slf4j;
import com.example.desarrollo.Events.NotificacionCorreoEvent;
import com.example.desarrollo.dto.ArrendadorRequestDTO;
import com.example.desarrollo.dto.ArrendadorResponseDTO;
import com.example.desarrollo.dto.ArrendadorUpdateRequestDTO;
import com.example.desarrollo.exceptions.ResourceNotFoundException;
import com.example.desarrollo.model.Arrendador;
import com.example.desarrollo.model.Calificacion;
import com.example.desarrollo.model.Mail;
import com.example.desarrollo.repository.ArrendadorRepository;
import com.example.desarrollo.repository.CalificacionRepository;
import com.example.desarrollo.repository.HabitacionRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ArrendadorService {
    private final ArrendadorRepository arrendadorRepository;
    private final ModelMapper modelMapper;
    private final UsuarioService usuarioService;
    private final CalificacionRepository calificacionRepository;
    private final HabitacionRepository habitacionRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public ArrendadorService(ArrendadorRepository arrendadorRepository, ModelMapper modelMapper,
                             UsuarioService usuarioService, CalificacionRepository calificacionRepository,
                             HabitacionRepository habitacionRepository, ApplicationEventPublisher publisher) {
        this.arrendadorRepository = arrendadorRepository;
        this.modelMapper = modelMapper;
        this.usuarioService = usuarioService;
        this.calificacionRepository = calificacionRepository;
        this.habitacionRepository = habitacionRepository;
        this.publisher = publisher;
    }

    // Read (GET)
    public ArrendadorResponseDTO findById(Long id) {
        return modelMapper.map(buscar(id), ArrendadorResponseDTO.class);
    }

    // Lista paginada de todos los arrendadores (solo ADMIN, regla en el controller)
    public Page<ArrendadorResponseDTO> getAll(Pageable pageable) {
        return arrendadorRepository.findAll(pageable)
                .map(a -> modelMapper.map(a, ArrendadorResponseDTO.class));
    }

    @Transactional
    public ArrendadorResponseDTO update(Long id, ArrendadorUpdateRequestDTO dto) {
        usuarioService.validarQueSoyYo(id);
        Arrendador arrendador = buscar(id);
        arrendador.setNombre(dto.getNombre());
        // Only replace the DNI photo when a new one is sent (empty = keep the current one)
        if (dto.getDniFoto() != null && !dto.getDniFoto().isBlank()) {
            arrendador.setDniFoto(dto.getDniFoto());
        }
        log.info("Arrendador {} actualizó su perfil", id);
        return modelMapper.map(arrendador, ArrendadorResponseDTO.class);
    }

    // Solo el ADMIN (regla en SecurityConfig): aprueba al arrendador tras revisar su DNI
    @Transactional
    public ArrendadorResponseDTO verificar(Long id) {
        Arrendador arrendador = buscar(id);
        arrendador.setVerificado(true);
        log.info("Arrendador {} verificado por un administrador", id);

        publisher.publishEvent(new NotificacionCorreoEvent(this, Mail.para(arrendador.getCorreo(),
                "MuvU: tu cuenta fue verificada",
                "Hola " + arrendador.getNombre() + ", verificamos tu identidad. Ya puedes publicar tus habitaciones en MuvU.")));
        return modelMapper.map(arrendador, ArrendadorResponseDTO.class);
    }

    private Arrendador buscar(Long id) {
        return arrendadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Arrendador no encontrado con ID: " + id));
    }



    public void deleteById(Long id) {
        usuarioService.validarQueSoyYo(id);
        if (!arrendadorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Arrendador no encontrado con ID: " + id);
        }
        arrendadorRepository.deleteById(id);
        log.info("Arrendador {} eliminado", id);
    }

    // Recalcula el promedio con todas las calificaciones de las habitaciones del arrendador
    @Transactional
    public void actualizarPromedio(Long arrendadorId) {
        Arrendador arrendador = arrendadorRepository.findById(arrendadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Arrendador no encontrado con ID: " + arrendadorId));

        List<Calificacion> calificaciones = calificacionRepository.findByReceptorArrendadorId(arrendadorId);

        double promedio = calificaciones.stream()
                .mapToInt(Calificacion::getPuntuacion)
                .average()
                .orElse(0.0);

        arrendador.setPuntajePromedio(Math.round(promedio * 10) / 10.0);
        arrendador.setTotalCalificaciones((long) calificaciones.size());
        log.info("Promedio del arrendador {} actualizado a {} ({} calificaciones)",
                arrendadorId, arrendador.getPuntajePromedio(), calificaciones.size());
    }

    @Transactional
    public void actualizarCantidadHabitaciones(Long arrendadorId) {
        Arrendador arrendador = buscar(arrendadorId);
        arrendador.setCantidadHabitaciones(habitacionRepository.countByArrendadorId(arrendadorId));
        log.info("Arrendador {} ahora tiene {} habitaciones", arrendadorId, arrendador.getCantidadHabitaciones());
    }
}
