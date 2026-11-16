package com.example.desarrollo.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private ArrendadorRepository arrendadorRepository;
    @Mock private UniversidadRepository universidadRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private ApplicationEventPublisher publisher;

    @InjectMocks private AuthService authService;

    @Test
    void registrarEstudiante_guardaClaveCifradaYDevuelveToken() {
        Universidad utec = new Universidad();
        utec.setId(1L);
        when(usuarioRepository.existsByCorreo("ana@utec.edu.pe")).thenReturn(false);
        when(universidadRepository.findById(1L)).thenReturn(Optional.of(utec));
        when(passwordEncoder.encode("Clave123!")).thenReturn("hash");
        when(jwtService.generateToken(any(Estudiante.class))).thenReturn("jwt");

        TokenResponseDTO respuesta = authService.registerEstudiante(
                new EstudianteRequestDTO("Ana", "ana@utec.edu.pe", "Clave123!", 1L, null));

        ArgumentCaptor<Estudiante> captor = ArgumentCaptor.forClass(Estudiante.class);
        verify(estudianteRepository).save(captor.capture());
        assertEquals("hash", captor.getValue().getContrasena());
        assertEquals(utec, captor.getValue().getUniversidad());
        assertEquals("jwt", respuesta.token());
        assertEquals("ESTUDIANTE", respuesta.rol());
        verify(publisher).publishEvent(any(NotificacionCorreoEvent.class));
    }

    @Test
    void registrarEstudiante_conCorreoRepetido_lanzaDuplicate() {
        when(usuarioRepository.existsByCorreo("ana@utec.edu.pe")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.registerEstudiante(
                new EstudianteRequestDTO("Ana", "ana@utec.edu.pe", "Clave123!", 1L, null)));
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    void registrarEstudiante_conUniversidadInexistente_lanzaNotFound() {
        when(usuarioRepository.existsByCorreo("ana@utec.edu.pe")).thenReturn(false);
        when(universidadRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.registerEstudiante(
                new EstudianteRequestDTO("Ana", "ana@utec.edu.pe", "Clave123!", 9L, null)));
    }

    @Test
    void registrarArrendador_quedaSinVerificarYRecibeBienvenida() {
        when(usuarioRepository.existsByCorreo("rosa@muvu.com")).thenReturn(false);
        when(passwordEncoder.encode("Clave123!")).thenReturn("hash");
        when(jwtService.generateToken(any(Arrendador.class))).thenReturn("jwt");

        TokenResponseDTO respuesta = authService.registerArrendador(
                new ArrendadorRequestDTO("Rosa", "rosa@muvu.com", "Clave123!", null));

        ArgumentCaptor<Arrendador> captor = ArgumentCaptor.forClass(Arrendador.class);
        verify(arrendadorRepository).save(captor.capture());
        assertFalse(captor.getValue().getVerificado());
        assertEquals("ARRENDADOR", respuesta.rol());
        verify(publisher).publishEvent(any(NotificacionCorreoEvent.class));
    }

    @Test
    void login_conCredencialesValidas_devuelveToken() {
        Usuario ana = new Estudiante();
        ana.setId(3L);
        ana.setRol(Rol.ESTUDIANTE);
        when(usuarioRepository.findByCorreo("ana@utec.edu.pe")).thenReturn(Optional.of(ana));
        when(jwtService.generateToken(ana)).thenReturn("jwt");

        TokenResponseDTO respuesta = authService.login(login("ana@utec.edu.pe", "Clave123!"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertEquals("jwt", respuesta.token());
        assertEquals(3L, respuesta.id());
    }

    @Test
    void login_conClaveIncorrecta_propagaBadCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("mal"));

        assertThrows(BadCredentialsException.class, () -> authService.login(login("ana@utec.edu.pe", "otra")));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void refresh_conTokenValido_devuelveTokensNuevos() {
        Usuario ana = estudiante();
        when(jwtService.isRefreshTokenValid("refresh")).thenReturn(true);
        when(jwtService.extractUserId("refresh")).thenReturn(3L);
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(ana));
        when(jwtService.generateToken(ana)).thenReturn("jwt-nuevo");
        when(jwtService.generateRefreshToken(ana)).thenReturn("refresh-nuevo");

        TokenResponseDTO respuesta = authService.refresh(refresh("refresh"));

        assertEquals("jwt-nuevo", respuesta.token());
        assertEquals("refresh-nuevo", respuesta.refreshToken());
    }

    @Test
    void refresh_conTokenInvalido_lanzaInvalidToken() {
        when(jwtService.isRefreshTokenValid("malo")).thenReturn(false);

        assertThrows(InvalidTokenException.class, () -> authService.refresh(refresh("malo")));
    }

    @Test
    void refresh_deUsuarioEliminado_lanzaInvalidToken() {
        when(jwtService.isRefreshTokenValid("refresh")).thenReturn(true);
        when(jwtService.extractUserId("refresh")).thenReturn(9L);
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> authService.refresh(refresh("refresh")));
    }

    @Test
    void forgotPassword_correoRegistrado_enviaCorreoConToken() {
        Usuario ana = estudiante();
        when(usuarioRepository.findByCorreo("ana@utec.edu.pe")).thenReturn(Optional.of(ana));
        when(jwtService.generateResetToken(ana)).thenReturn("reset");

        authService.forgotPassword(forgot("ana@utec.edu.pe"));

        verify(publisher).publishEvent(any(NotificacionCorreoEvent.class));
    }

    @Test
    void forgotPassword_correoNoRegistrado_noHaceNada() {
        when(usuarioRepository.findByCorreo("nadie@utec.edu.pe")).thenReturn(Optional.empty());

        authService.forgotPassword(forgot("nadie@utec.edu.pe"));

        verify(publisher, never()).publishEvent(any(NotificacionCorreoEvent.class));
    }

    @Test
    void resetPassword_conTokenValido_cambiaLaContrasena() {
        Usuario ana = estudiante();
        when(jwtService.isResetTokenValid("reset")).thenReturn(true);
        when(jwtService.extractUserId("reset")).thenReturn(3L);
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(ana));
        when(passwordEncoder.encode("Nueva123!")).thenReturn("hash-nuevo");

        authService.resetPassword(reset("reset", "Nueva123!"));

        assertEquals("hash-nuevo", ana.getContrasena());
    }

    @Test
    void resetPassword_conTokenInvalido_lanzaInvalidToken() {
        when(jwtService.isResetTokenValid("malo")).thenReturn(false);

        assertThrows(InvalidTokenException.class, () -> authService.resetPassword(reset("malo", "Nueva123!")));
        verify(passwordEncoder, never()).encode(any());
    }

    private Usuario estudiante() {
        Usuario ana = new Estudiante();
        ana.setId(3L);
        ana.setCorreo("ana@utec.edu.pe");
        ana.setNombre("Ana");
        ana.setRol(Rol.ESTUDIANTE);
        return ana;
    }

    private RefreshTokenRequestDTO refresh(String token) {
        RefreshTokenRequestDTO dto = new RefreshTokenRequestDTO();
        dto.setRefreshToken(token);
        return dto;
    }

    private ForgotPasswordRequestDTO forgot(String correo) {
        ForgotPasswordRequestDTO dto = new ForgotPasswordRequestDTO();
        dto.setCorreo(correo);
        return dto;
    }

    private ResetPasswordRequestDTO reset(String token, String nuevaContrasena) {
        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        dto.setToken(token);
        dto.setNuevaContrasena(nuevaContrasena);
        return dto;
    }

    private LoginRequestDTO login(String correo, String contrasena) {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setCorreo(correo);
        dto.setContrasena(contrasena);
        return dto;
    }
}
