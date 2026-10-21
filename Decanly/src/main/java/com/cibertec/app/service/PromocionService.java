package com.cibertec.app.service;
import com.cibertec.app.entity.*;
// Integrante 1: promociones. Firmas iniciales: completar parámetros según la guía.
public interface PromocionService {
    void guardar(Pack pack);
    void actualizar(Long id, Pack pack);
    void retirar(Long id);
}
