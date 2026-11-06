package com.cibertec.app.service;

import java.util.List;
import com.cibertec.app.dto.PedidoFormulario;
import com.cibertec.app.entity.CarritoItem;
import com.cibertec.app.entity.Pedido;

public interface PedidoService {
    Pedido prepararSolicitud(String nombre, String telefono, String direccion,
            boolean aceptaTerminos, List<CarritoItem> carrito);
    Long registrarManual(PedidoFormulario formulario);
    Pedido buscar(Long id);
}
