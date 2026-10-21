package com.cibertec.app.service;
import com.cibertec.app.entity.*;
// Integrante 4: login e integración. Firmas iniciales: completar parámetros según la guía.
public interface UsuarioService {
    boolean login(String correo, String clave);
    void cerrarSesion();
    void guardar(Usuario usuario);
}
