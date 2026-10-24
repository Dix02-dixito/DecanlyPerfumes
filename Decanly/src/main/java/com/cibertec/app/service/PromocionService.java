package com.cibertec.app.service;

import java.util.List;
import com.cibertec.app.entity.Pack;

public interface PromocionService {

    List<Pack> listar();

    void guardar(
            Long id,
            Pack datos,
            List<Long> productoIds,
            List<Integer> ml,
            List<Integer> cantidades);

    void cambiarEstado(Long id, Integer version, boolean activo);
}