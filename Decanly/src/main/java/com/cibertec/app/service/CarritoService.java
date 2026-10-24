package com.cibertec.app.service;

import java.util.List;
import com.cibertec.app.entity.CarritoItem;
import com.cibertec.app.dto.SeleccionPack;

public interface CarritoService {

    List<CarritoItem> agregarProducto(
            Long productoId,
            Integer ml,
            Integer cantidad,
            List<CarritoItem> actuales);

    List<CarritoItem> agregarPromocion(
            Long packId,
            List<CarritoItem> actuales);
    
    List<CarritoItem> agregarPackPersonalizado(
            List<SeleccionPack> seleccion,
            List<CarritoItem> actuales);
}