package com.cibertec.app.service.impl;
import com.cibertec.app.entity.Usuario;
import com.cibertec.app.service.UsuarioService;
import org.springframework.stereotype.Service;
@Service
public class UsuarioServiceImpl implements UsuarioService {
    // TODO integrante 4: login e integración: completar; aún no cambia MySQL.
    @Override public boolean login(String correo, String clave) {
        throw new UnsupportedOperationException("Pendiente: Usuario.login");
    }
    // TODO integrante 4: login e integración: completar; aún no cambia MySQL.
    @Override public void cerrarSesion() {
        throw new UnsupportedOperationException("Pendiente: Usuario.cerrarSesion");
    }
    // TODO integrante 4: login e integración: completar; aún no cambia MySQL.
    @Override public void guardar(Usuario usuario) {
        throw new UnsupportedOperationException("Pendiente: Usuario.guardar");
    }
}
