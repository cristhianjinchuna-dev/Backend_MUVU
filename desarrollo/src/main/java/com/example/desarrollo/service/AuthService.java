package com.example.desarrollo.service;

import lombok.extern.slf4j.Slf4j;
import com.example.desarrollo.Events.NotificacionCorreoEvent;
import com.example.desarrollo.dto.ArrendadorRequestDTO;
import com.example.desarrollo.dto.EstudianteRequestDTO;
import com.example.desarrollo.dto.Logueo.ForgotPasswordRequestDTO;
import com.example.desarrollo.dto.Logueo.LoginRequestDTO;
import com.example.desarrollo.dto.Logueo.RefreshTokenRequestDTO;
import com.example.desarrollo.dto.Logueo.ResetPasswordRequestDTO;
import com.example.desarrollo.dto.Logueo.TokenResponseDTO;
import com.example.desarrollo.exceptions.DuplicateResourceException;
import com.example.desarrollo.exceptions.InvalidTokenException;
import com.example.desarrollo.exceptions.ResourceNotFoundException;
import com.example.desarrollo.model.*;
import com.example.desarrollo.repository.ArrendadorRepository;
import com.example.desarrollo.repository.EstudianteRepository;
import com.example.desarrollo.repository.UniversidadRepository;
import com.example.desarrollo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final ArrendadorRepository arrendadorRepository;
    private final UniversidadRepository universidadRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ApplicationEventPublisher publisher;
    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public TokenResponseDTO registerEstudiante(EstudianteRequestDTO dto) {
        validarCorreoLibre(dto.getCorreo());
        Universidad uni = universidadRepository.findById(dto.getUniversidadId())
                .orElseThrow(() -> new ResourceNotFoundException("Universidad no encontrada"));

        Estudiante e = new Estudiante();
        e.setNombre(dto.getNombre());
        e.setCorreo(dto.getCorreo());
        e.setContrasena(passwordEncoder.encode(dto.getContrasena()));
        e.setRol(Rol.ESTUDIANTE);
        e.setUniversidad(uni);
        if (dto.getTelefono() != null && !dto.getTelefono().isBlank()) {
            e.setTelefono(dto.getTelefono());
        }

        estudianteRepository.save(e);
        log.info("Estudiante {} registrado en la universidad {}", e.getId(), uni.getId());
        enviarBienvenida(e, "Ya puedes buscar y reservar habitaciones cerca de tu universidad.");
        return generarRespuesta(e);
    }

    @Transactional
    public TokenResponseDTO registerArrendador(ArrendadorRequestDTO dto) {
        validarCorreoLibre(dto.getCorreo());

        Arrendador a = new Arrendador();
        a.setNombre(dto.getNombre());
        a.setCorreo(dto.getCorreo());
        a.setContrasena(passwordEncoder.encode(dto.getContrasena()));
        a.setRol(Rol.ARRENDADOR);
        if (dto.getTelefono() != null && !dto.getTelefono().isBlank()) {
            a.setTelefono(dto.getTelefono());
        }

        arrendadorRepository.save(a);
        log.info("Arrendador {} registrado, pendiente de verificación", a.getId());
        enviarBienvenida(a, "Un administrador revisará tu cuenta; cuando esté verificada podrás publicar tus habitaciones.");
        return generarRespuesta(a);
    }

    public TokenResponseDTO login(LoginRequestDTO dto) {
        // Lanza BadCredentialsException si el correo o la contraseña están mal
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getCorreo(), dto.getContrasena()));

        Usuario u = usuarioRepository.findByCorreo(dto.getCorreo()).orElseThrow();
        log.info("Usuario {} inició sesión con rol {}", u.getId(), u.getRol());
        return generarRespuesta(u);
    }

    public TokenResponseDTO refresh(RefreshTokenRequestDTO dto) {
        String refreshToken = dto.getRefreshToken();
        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            throw new InvalidTokenException("El refresh token es inválido o expiró");
        }
        Usuario usuario = usuarioRepository.findById(jwtService.extractUserId(refreshToken))
                .orElseThrow(() -> new InvalidTokenException("El usuario del token ya no existe"));
        return generarRespuesta(usuario);
    }

    // Si el correo existe le envía un token de recuperación; si no, no hace nada (no revela qué correos están registrados)
    @Transactional
    public void forgotPassword(ForgotPasswordRequestDTO dto) {
        usuarioRepository.findByCorreo(dto.getCorreo()).ifPresent(usuario -> {
            String token = jwtService.generateResetToken(usuario);
            String enlace = frontendUrl + "/reset-password?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
            publisher.publishEvent(new NotificacionCorreoEvent(this, Mail.conAccion(usuario.getCorreo(),
                    "MuvU: restablecer tu contraseña",
                    "Hola " + usuario.getNombre() + ", recibimos una solicitud para restablecer tu contraseña. "
                            + "Haz clic en el botón para crear una nueva. El enlace vence en 15 minutos. "
                            + "Si no fuiste tú, ignora este correo.",
                    enlace, "Restablecer contraseña")));
            log.info("Enlace de recuperación enviado al usuario {}", usuario.getId());
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDTO dto) {
        if (!jwtService.isResetTokenValid(dto.getToken())) {
            throw new InvalidTokenException("El token de recuperación es inválido o expiró");
        }
        Usuario usuario = usuarioRepository.findById(jwtService.extractUserId(dto.getToken()))
                .orElseThrow(() -> new InvalidTokenException("El usuario del token ya no existe"));
        usuario.setContrasena(passwordEncoder.encode(dto.getNuevaContrasena()));
        log.info("Usuario {} restableció su contraseña", usuario.getId());
        publisher.publishEvent(new NotificacionCorreoEvent(this, Mail.para(usuario.getCorreo(),
                "MuvU: tu contraseña fue cambiada",
                "Hola " + usuario.getNombre() + ", tu contraseña se cambió correctamente. "
                        + "Si no fuiste tú, contacta al administrador.")));
    }

    // Access token + refresh token nuevos (el refresh también se renueva)
    private TokenResponseDTO generarRespuesta(Usuario usuario) {
        return new TokenResponseDTO(jwtService.generateToken(usuario), jwtService.generateRefreshToken(usuario),
                usuario.getRol().name(), usuario.getId());
    }

    private void enviarBienvenida(Usuario usuario, String mensaje) {
        publisher.publishEvent(new NotificacionCorreoEvent(this, Mail.para(usuario.getCorreo(),
                "¡Bienvenido a MuvU!",
                "Hola " + usuario.getNombre() + ", tu cuenta fue creada con éxito. " + mensaje)));
    }

    private void validarCorreoLibre(String correo) {
        if (usuarioRepository.existsByCorreo(correo)) {
            log.warn("Intento de registro con un correo ya registrado");
            throw new DuplicateResourceException("El correo " + correo + " ya está registrado");
        }
    }
}
