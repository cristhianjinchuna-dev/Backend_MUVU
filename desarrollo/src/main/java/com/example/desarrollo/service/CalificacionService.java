package com.example.desarrollo.service;

import lombok.extern.slf4j.Slf4j;
import com.example.desarrollo.Events.ActualizacionPromedioEvent;
import com.example.desarrollo.dto.CalificacionRequestDTO;
import com.example.desarrollo.dto.CalificacionResponseDTO;
import org.springframework.context.ApplicationEventPublisher;
import com.example.desarrollo.exceptions.DuplicateResourceException;
import com.example.desarrollo.exceptions.ForbiddenException;
import com.example.desarrollo.exceptions.ReservaInvalidStateException;
import com.example.desarrollo.exceptions.ResourceNotFoundException;
import com.example.desarrollo.model.*;
import com.example.desarrollo.repository.CalificacionRepository;
import com.example.desarrollo.repository.EstudianteRepository;
import com.example.desarrollo.repository.HabitacionRepository;
import com.example.desarrollo.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalificacionService {

    private final CalificacionRepository calificacionRepository;
    private final ModelMapper modelMapper;
    private final EstudianteRepository estudianteRepository;
    private final ReservaRepository reservaRepository;
    private final HabitacionRepository habitacionRepository;
    private final UsuarioService usuarioService;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public CalificacionResponseDTO create(CalificacionRequestDTO dto) {
        Reserva reserva = reservaRepository.findById(dto.getReservaId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe reserva con el ID: " + dto.getReservaId()));

        Long miId = usuarioService.getIdUsuarioActual();
        Estudiante autor = estudianteRepository.findById(miId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));


        if (reserva.getEstudiante() == null || !reserva.getEstudiante().getId().equals(autor.getId())) {
            throw new ForbiddenException("El estudiante con ID: " + autor.getId()
                    + " no es el titular de la reserva con ID: " + reserva.getId());
        }

        if (reserva.getEstado() != Estado.CONFIRMADO) {
            throw new ReservaInvalidStateException("Solo se puede calificar una reserva confirmada");
        }

        if (calificacionRepository.existsByReservaIdAndAutorId(reserva.getId(), autor.getId())) {
            throw new DuplicateResourceException("La reserva con ID: " + reserva.getId() + " ya fue calificada");
        }

        Calificacion newCalificacion = new Calificacion();
        newCalificacion.setPuntuacion(dto.getPuntuacion());
        newCalificacion.setDescripcion(dto.getDescripcion());
        newCalificacion.setReserva(reserva);

        newCalificacion.setAutor(autor);

        Habitacion habitacion = reserva.getHabitacion();
        newCalificacion.setReceptor(habitacion);
        newCalificacion = calificacionRepository.save(newCalificacion);
        log.info("Calificación {} creada por el estudiante {} para la habitación {}",
                newCalificacion.getId(), autor.getId(), habitacion.getId());

        publisher.publishEvent(new ActualizacionPromedioEvent(this, habitacion.getArrendador().getId()));

        return toResponseDTO(newCalificacion);
    }

    public CalificacionResponseDTO findById(Long id) {
        Calificacion calificacion = calificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe calificacion con el ID: " + id));
        return toResponseDTO(calificacion);
    }

    public Page<CalificacionResponseDTO> findByHabitacion(Long habitacionId, Pageable pageable) {
        if (!habitacionRepository.existsById(habitacionId)) {
            throw new ResourceNotFoundException("No existe habitacion con el ID: " + habitacionId);
        }
        return calificacionRepository.findByReceptorId(habitacionId, pageable).map(this::toResponseDTO);
    }


    public Page<CalificacionResponseDTO> findByEstudiante(Long estudianteId, Pageable pageable) {
        if (!estudianteRepository.existsById(estudianteId)) {
            throw new ResourceNotFoundException("No existe estudiante con el ID: " + estudianteId);
        }
        return calificacionRepository.findByAutorId(estudianteId, pageable).map(this::toResponseDTO);
    }

    @Transactional
    public void deleteById(Long id) {
        Calificacion calificacion = calificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe calificacion con el ID: " + id));

        usuarioService.validarQueSoyYo(calificacion.getAutor().getId());
        calificacionRepository.delete(calificacion);
        log.info("Calificación {} eliminada", id);

        publisher.publishEvent(new ActualizacionPromedioEvent(this, calificacion.getReceptor().getArrendador().getId()));
    }

    // En la entidad la habitación se llama "receptor", por eso ModelMapper no llena habitacionId
    private CalificacionResponseDTO toResponseDTO(Calificacion c) {
        CalificacionResponseDTO dto = modelMapper.map(c, CalificacionResponseDTO.class);
        dto.setHabitacionId(c.getReceptor().getId());
        if (c.getAutor() != null) {
            dto.setAutorFotoPerfil(c.getAutor().getFotoPerfil());
        }
        return dto;
    }
}
