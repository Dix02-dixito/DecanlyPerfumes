package com.cibertec.app.service;

import java.math.BigDecimal;

public interface PedidoService {

    void registrar(
            String nombre,
            String telefono,
            String direccion,
            BigDecimal total);

    void cancelar(Long id);

    void cambiarEstado(
            Long id,
            String estado);
}