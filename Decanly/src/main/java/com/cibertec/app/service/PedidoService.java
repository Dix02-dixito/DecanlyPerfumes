package com.cibertec.app.service;

import java.util.List;
import com.cibertec.app.entity.CarritoItem;
import java.math.BigDecimal;

public interface PedidoService {

	Long registrar(
	        String nombre,
	        String telefono,
	        String direccion,
	        boolean aceptaTerminos,
	        List<CarritoItem> carrito);

    void cancelar(Long id);

    void cambiarEstado(
            Long id,
            String estado);
}