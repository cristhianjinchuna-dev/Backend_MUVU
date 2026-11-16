package com.example.desarrollo.service;

import lombok.extern.slf4j.Slf4j;
import com.example.desarrollo.exceptions.ForbiddenException;
import com.example.desarrollo.model.Rol;
import com.example.desarrollo.model.Usuario;
import com.example.desarrollo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("No existe usuario con correo: " + correo));
    }

    // Devuelve el id del usuario logueado (sacado del token).

    public Long getIdUsuarioActual() {
        Usuario u = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return u.getId();
    }

    // true si quien consulta es el mismo usuario o un ADMIN (para mostrar datos privados como el celular)
    public boolean puedeVerDatosPrivados(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Usuario yo)) return false;
        return yo.getRol() == Rol.ADMIN || yo.getId().equals(id);
    }

    public void validarQueSoyYo(Long id) {
        Usuario yo = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (yo.getRol() == Rol.ADMIN) return; // El rol admin siempre tiene permisos
        if (!yo.getId().equals(id)) {
            log.warn("Usuario {} intentó modificar datos del usuario {}", yo.getId(), id);
            throw new ForbiddenException("No puedes modificar datos de otro usuario");
        }
    }

}
