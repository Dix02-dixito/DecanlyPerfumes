package com.cibertec.app.service;

import java.util.List;
import com.cibertec.app.dto.ResumenPack;
import com.cibertec.app.dto.SeleccionPack;

public interface PackPersonalizadoService {

    List<SeleccionPack> agregar(
            Long productoId,
            Integer ml,
            List<SeleccionPack> actuales);

    List<SeleccionPack> quitar(
            Long productoId,
            List<SeleccionPack> actuales);
    
    ResumenPack resumir(List<SeleccionPack> seleccion);
}