package com.example.desarrollo.service;

import lombok.extern.slf4j.Slf4j;
import com.example.desarrollo.Events.NotificacionCorreoEvent;
import com.example.desarrollo.dto.ContactoDTO;
import com.example.desarrollo.dto.ReservaRequestDTO;
import com.example.desarrollo.dto.ReservaResponseDTO;
import com.example.desarrollo.exceptions.ConflictException;
import com.example.desarrollo.exceptions.ForbiddenException;
import com.example.desarrollo.exceptions.InvalidOperationException;
import com.example.desarrollo.exceptions.ReservaInvalidStateException;
import com.example.desarrollo.exceptions.ResourceNotFoundException;
import com.example.desarrollo.model.*;
import com.example.desarrollo.repository.EstudianteRepository;
import com.example.desarrollo.repository.HabitacionRepository;
import com.example.desarrollo.repository.ReservaRepository;
import com.example.desarrollo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservaService {
    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final HabitacionRepository habitacionRepository;
    private final EstudianteRepository estudianteRepository;
    private final UsuarioService usuarioService;
    private final ApplicationEventPublisher publisher;

    @Transactional(readOnly = true)
    public ReservaResponseDTO findById(Long id) {
        Reserva reserva = buscarReserva(id);
        Long miId = usuarioService.getIdUsuarioActual();

        boolean soyElEstudiante = reserva.getEstudiante() != null
                && reserva.getEstudiante().getId().equals(miId);
        boolean soyElDueno = reserva.getHabitacion().getArrendador().getId().equals(miId);

        if (!soyElEstudiante && !soyElDueno) {
            throw new ForbiddenException("No tienes acceso a esta reserva");
        }
        return toDTO(reserva, miId);
    }

    @Transactional(readOnly = true)
    public Page<ReservaResponseDTO> findMisReservas(Pageable pageable) {
        Long miId = usuarioService.getIdUsuarioActual();
        Usuario yo = usuarioRepository.findById(miId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Page<Reserva> reservas = switch (yo.getRol()) {
            case ESTUDIANTE -> reservaRepository.findByEstudianteId(miId, pageable);
            case ARRENDADOR -> reservaRepository.findByHabitacionArrendadorId(miId, pageable);
            case ADMIN      -> reservaRepository.findAll(pageable);
        };

        return reservas.map(r -> toDTO(r, miId));
    }

    @Transactional
    public ReservaResponseDTO createReserva(ReservaRequestDTO dto) {
        Long miId = usuarioService.getIdUsuarioActual();
        Estudiante yo = estudianteRepository.findById(miId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro estudiante con id: " + miId));

        Habitacion habitacion = habitacionRepository.findById(dto.getHabitacionId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro habitacion con id: " + dto.getHabitacionId()));

        validarFechas(habitacion.getId(), dto.getFecha_inicio(), dto.getFecha_fin());

        Reserva nueva = new Reserva(dto.getFecha_inicio(), dto.getFecha_fin(), yo, habitacion);
        nueva.setMensaje(dto.getMensaje() == null || dto.getMensaje().isBlank() ? null : dto.getMensaje().trim());
        Reserva reserva = reservaRepository.save(nueva);
        log.info("Reserva {} creada por el estudiante {} para la habitación {}", reserva.getId(), miId, habitacion.getId());

        notificar(yo.getCorreo(), "MuvU: solicitud de reserva enviada",
                "Hola " + yo.getNombre() + ", enviaste una solicitud para " + detalle(reserva)
                        + ". Te avisaremos cuando el arrendador la confirme.");
        String mensajeDelEstudiante = reserva.getMensaje() != null
                ? "\n\nMensaje de " + yo.getNombre() + ":\n\"" + reserva.getMensaje() + "\""
                : "";
        notificar(habitacion.getArrendador().getCorreo(), "MuvU: nueva solicitud de reserva",
                yo.getNombre() + " quiere reservar " + detalle(reserva) + "." + mensajeDelEstudiante
                        + "\n\nIngresa a MuvU para revisar su perfil y confirmarla.");
        return toDTO(reserva, miId);
    }

    // Solo el estudiante que hizo la reserva puede cancelarla
    @Transactional
    public ReservaResponseDTO cancelReserva(Long id) {
        Reserva reserva = buscarReserva(id);
        Long miId = usuarioService.getIdUsuarioActual();

        if (reserva.getEstudiante() == null || !reserva.getEstudiante().getId().equals(miId)) {
            throw new ForbiddenException("Solo el estudiante que hizo la reserva puede cancelarla");
        }
        if (reserva.getEstado() == Estado.CANCELADO) {
            throw new ReservaInvalidStateException("No se puede cancelar una reserva ya cancelada");
        } else if (reserva.getEstado() == Estado.CONFIRMADO) {
            throw new ReservaInvalidStateException("No se puede cancelar una reserva ya confirmada");
        }

        reserva.setEstado(Estado.CANCELADO);
        log.info("Reserva {} cancelada", id);
        notificar(reserva.getHabitacion().getArrendador().getCorreo(), "MuvU: reserva cancelada",
                reserva.getEstudiante().getNombre() + " canceló su solicitud para " + detalle(reserva) + ".");
        return toDTO(reservaRepository.save(reserva), miId);
    }

    // Solo el arrendador dueño de la habitación puede confirmar
    @Transactional
    public ReservaResponseDTO confirmReserva(Long id) {
        Reserva reserva = buscarReserva(id);
        Long miId = usuarioService.getIdUsuarioActual();

        if (!reserva.getHabitacion().getArrendador().getId().equals(miId)) {
            throw new ForbiddenException("Solo el dueño de la habitación puede confirmar la reserva");
        }
        if (reserva.getEstado() == Estado.CANCELADO) {
            throw new ReservaInvalidStateException("No se puede confirmar una reserva ya cancelada");
        } else if (reserva.getEstado() == Estado.CONFIRMADO) {
            throw new ReservaInvalidStateException("No se puede confirmar una reserva ya confirmada");
        }

        reserva.setEstado(Estado.CONFIRMADO);
        log.info("Reserva {} confirmada", id);
        // Ya hay acuerdo: cada parte recibe los datos de contacto de la otra para coordinar la mudanza
        Estudiante estudiante = reserva.getEstudiante();
        Arrendador arrendador = reserva.getHabitacion().getArrendador();
        notificar(estudiante.getCorreo(), "MuvU: tu reserva fue confirmada",
                "Hola " + estudiante.getNombre() + ", el arrendador confirmó tu reserva para " + detalle(reserva) + ".\n\n"
                        + datosDeContacto("arrendador", arrendador)
                        + "\n\nEscríbele para coordinar la entrega de llaves y el pago.");
        notificar(arrendador.getCorreo(), "MuvU: confirmaste una reserva",
                "Hola " + arrendador.getNombre() + ", confirmaste la reserva de " + estudiante.getNombre()
                        + " para " + detalle(reserva) + ".\n\n"
                        + datosDeContacto("estudiante", estudiante)
                        + "\n\nEscríbele para coordinar la entrega de llaves y el pago.");
        return toDTO(reservaRepository.save(reserva), miId);
    }

    // ---------- helpers ----------

    private void notificar(String correo, String asunto, String mensaje) {
        publisher.publishEvent(new NotificacionCorreoEvent(this, Mail.para(correo, asunto, mensaje)));
    }

    private String datosDeContacto(String rol, Usuario u) {
        String telefono = u.getTelefono() != null ? u.getTelefono() : "no registrado";
        return "Datos de contacto del " + rol + ":\n"
                + "• Nombre: " + u.getNombre() + "\n"
                + "• Correo: " + u.getCorreo() + "\n"
                + "• Celular / WhatsApp: " + telefono;
    }

    private String detalle(Reserva r) {
        return "la habitación en " + r.getHabitacion().getDireccion()
                + " del " + r.getFecha_inicio() + " al " + r.getFecha_fin();
    }

    private void validarFechas(Long habitacionId, LocalDate inicio, LocalDate fin) {
        if (!fin.isAfter(inicio)) {
            throw new InvalidOperationException("La fecha de fin debe ser posterior a la fecha de inicio");
        }
        boolean ocupada = reservaRepository
                .findByHabitacionIdAndEstadoIn(habitacionId, List.of(Estado.PENDIENTE, Estado.CONFIRMADO))
                .stream()
                .anyMatch(r -> r.seCruzaCon(inicio, fin));
        if (ocupada) {
            log.warn("Habitación {} ya reservada entre {} y {}", habitacionId, inicio, fin);
            throw new ConflictException("La habitación ya está reservada en esas fechas");
        }
    }

    private Reserva buscarReserva(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe reserva con el ID: " + id));
    }

    // miId = usuario que hace la consulta; define de quién es el contacto que se muestra
    private ReservaResponseDTO toDTO(Reserva r, Long miId) {
        ReservaResponseDTO dto = new ReservaResponseDTO();
        dto.setId(r.getId());
        dto.setEstado(r.getEstado());
        dto.setFecha_inicio(r.getFecha_inicio());
        dto.setFecha_fin(r.getFecha_fin());
        dto.setHabitacionId(r.getHabitacion() != null ? r.getHabitacion().getId() : null);
        dto.setEstudianteId(r.getEstudiante() != null ? r.getEstudiante().getId() : null);
        dto.setMensaje(r.getMensaje());
        dto.setContacto(contactoPara(r, miId));
        return dto;
    }

    // Solo con la reserva CONFIRMADA: el estudiante ve al arrendador y el arrendador ve al estudiante.
    // Para cualquier otro (p. ej. ADMIN) no se comparten datos de contacto
    private ContactoDTO contactoPara(Reserva r, Long miId) {
        if (r.getEstado() != Estado.CONFIRMADO || miId == null) return null;
        Usuario estudiante = r.getEstudiante();
        Usuario arrendador = r.getHabitacion() != null ? r.getHabitacion().getArrendador() : null;
        if (estudiante != null && miId.equals(estudiante.getId())) return toContacto(arrendador);
        if (arrendador != null && miId.equals(arrendador.getId())) return toContacto(estudiante);
        return null;
    }

    private ContactoDTO toContacto(Usuario u) {
        if (u == null) return null;
        return new ContactoDTO(u.getId(), u.getNombre(), u.getCorreo(), u.getTelefono(), u.getFotoPerfil());
    }
}
