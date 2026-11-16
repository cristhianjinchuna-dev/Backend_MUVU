package com.example.desarrollo.service;

import com.example.desarrollo.Events.NotificacionCorreoEvent;
import com.example.desarrollo.dto.SuspensionRequestDTO;
import com.example.desarrollo.dto.VisibilidadRequestDTO;
import com.example.desarrollo.exceptions.InvalidOperationException;
import com.example.desarrollo.exceptions.ResourceNotFoundException;
import com.example.desarrollo.model.Habitacion;
import com.example.desarrollo.model.Mail;
import com.example.desarrollo.model.Rol;
import com.example.desarrollo.model.Usuario;
import com.example.desarrollo.repository.HabitacionRepository;
import com.example.desarrollo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Herramientas del ADMIN para contenido o conductas inapropiadas.
// No se borra nada: suspender/ocultar es reversible y conserva el historial (reservas, pagos, calificaciones)
@Slf4j
@Service
@RequiredArgsConstructor
public class ModeracionService {

    private final UsuarioRepository usuarioRepository;
    private final HabitacionRepository habitacionRepository;
    private final ApplicationEventPublisher publisher;

    // Suspendido: no puede iniciar sesión, sus tokens dejan de servir y, si es arrendador,
    // sus habitaciones desaparecen de las búsquedas y no se pueden reservar
    @Transactional
    public void cambiarSuspension(Long usuarioId, SuspensionRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));
        if (usuario.getRol() == Rol.ADMIN) {
            throw new InvalidOperationException("No se puede suspender a un administrador");
        }
        boolean suspender = dto.getSuspendido();
        if (Boolean.TRUE.equals(usuario.getSuspendido()) == suspender) return; // ya estaba así

        usuario.setSuspendido(suspender);
        log.info("Usuario {} {} por un administrador", usuarioId, suspender ? "suspendido" : "reactivado");

        if (suspender) {
            String extra = usuario.getRol() == Rol.ARRENDADOR
                    ? " y tus habitaciones no aparecerán en las búsquedas" : "";
            notificar(usuario.getCorreo(), "MuvU: tu cuenta fue suspendida",
                    "Hola " + usuario.getNombre() + ", un administrador suspendió tu cuenta de MuvU."
                            + motivo(dto.getMotivo())
                            + "\n\nMientras esté suspendida no podrás iniciar sesión" + extra + "."
                            + " Si crees que es un error, responde a este correo.");
        } else {
            notificar(usuario.getCorreo(), "MuvU: tu cuenta fue reactivada",
                    "Hola " + usuario.getNombre() + ", tu cuenta de MuvU vuelve a estar activa. Ya puedes iniciar sesión."
                            + motivo(dto.getMotivo()));
        }
    }

    // Oculta: no sale en búsquedas ni se puede reservar; el arrendador la sigue viendo en su panel
    @Transactional
    public void cambiarVisibilidad(Long habitacionId, VisibilidadRequestDTO dto) {
        Habitacion habitacion = habitacionRepository.findById(habitacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Habitación no encontrada con ID: " + habitacionId));
        boolean ocultar = dto.getOculta();
        if (Boolean.TRUE.equals(habitacion.getOculta()) == ocultar) return;

        habitacion.setOculta(ocultar);
        log.info("Habitación {} {} por un administrador", habitacionId, ocultar ? "ocultada" : "publicada de nuevo");

        Usuario arrendador = habitacion.getArrendador();
        String asunto = ocultar ? "MuvU: ocultamos una de tus habitaciones" : "MuvU: tu habitación vuelve a estar publicada";
        String cuerpo = ocultar
                ? "Hola " + arrendador.getNombre() + ", un administrador ocultó tu habitación en "
                        + habitacion.getDireccion() + " porque no cumple las normas de MuvU." + motivo(dto.getMotivo())
                        + "\n\nYa no aparece en las búsquedas ni se puede reservar. Corrígela y responde a este correo para revisarla."
                : "Hola " + arrendador.getNombre() + ", tu habitación en " + habitacion.getDireccion()
                        + " vuelve a aparecer en las búsquedas de MuvU." + motivo(dto.getMotivo());
        notificar(arrendador.getCorreo(), asunto, cuerpo);
    }

    private String motivo(String motivo) {
        return motivo == null || motivo.isBlank() ? "" : "\n\nMotivo: " + motivo.trim();
    }

    private void notificar(String correo, String asunto, String mensaje) {
        publisher.publishEvent(new NotificacionCorreoEvent(this, Mail.para(correo, asunto, mensaje)));
    }
}
