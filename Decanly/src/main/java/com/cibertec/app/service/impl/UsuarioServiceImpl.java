package com.cibertec.app.service.impl;

import java.util.Optional;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.app.entity.Usuario;
import com.cibertec.app.repository.UsuarioRepository;
import com.cibertec.app.service.UsuarioService;

@Service
public class UsuarioServiceImpl
        implements UsuarioService, UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioServiceImpl(
            UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            return Optional.empty();
        }

        return usuarioRepository.findByCorreoIgnoreCase(
            correo.trim()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) {

        Usuario usuario = buscarPorCorreo(correo)
            .orElseThrow(() ->
                new UsernameNotFoundException(
                    "Credenciales incorrectas"
                )
            );

        return User.withUsername(usuario.getCorreo())
            .password(usuario.getClaveHash())
            .roles(usuario.getRol())
            .disabled(!usuario.isActivo())
            .build();
    }
}
