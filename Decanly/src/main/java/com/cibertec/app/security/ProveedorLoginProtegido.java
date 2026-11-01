package com.cibertec.app.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.dao.DataAccessException;

import com.cibertec.app.entity.Usuario;
import com.cibertec.app.service.UsuarioService;

public class ProveedorLoginProtegido implements AuthenticationProvider {
    private final AuthenticationProvider delegado;
    private final IntentosLogin intentos;
    private final UsuarioService usuarios;

    public ProveedorLoginProtegido(AuthenticationProvider delegado, IntentosLogin intentos, UsuarioService usuarios) {
        this.delegado = delegado;
        this.intentos = intentos;
        this.usuarios = usuarios;
    }

    @Override
    public Authentication authenticate(Authentication solicitud) {
        String direccion = solicitud.getDetails() instanceof WebAuthenticationDetails detalles
                ? detalles.getRemoteAddress() : null;
        String correo;
        try {
            // Usa la identidad real: MySQL puede considerar equivalentes correos con acentos distintos.
            correo = usuarios.buscarPorCorreo(solicitud.getName()).map(Usuario::getCorreo)
                    .orElse(solicitud.getName());
        } catch (DataAccessException ex) {
            throw new InternalAuthenticationServiceException("No se pudo comprobar el acceso.", ex);
        }
        return intentos.autenticar(correo, direccion, () -> delegado.authenticate(solicitud));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return delegado.supports(authentication);
    }
}
