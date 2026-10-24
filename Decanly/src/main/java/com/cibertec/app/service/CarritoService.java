package com.cibertec.app.service;

import java.util.List;
import com.cibertec.app.entity.CarritoItem;

public interface CarritoService {

    List<CarritoItem> agregarProducto(
            Long productoId,
            Integer ml,
            Integer cantidad,
            List<CarritoItem> actuales);

    List<CarritoItem> agregarPromocion(
            Long packId,
            List<CarritoItem> actuales);
}