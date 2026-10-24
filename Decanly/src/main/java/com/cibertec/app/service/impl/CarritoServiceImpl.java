package com.cibertec.app.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.app.entity.CarritoItem;
import com.cibertec.app.entity.Pack;
import com.cibertec.app.entity.PackDetalle;
import com.cibertec.app.entity.Presentacion;
import com.cibertec.app.entity.Producto;
import com.cibertec.app.service.CarritoService;
import com.cibertec.app.service.CatalogoService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarritoServiceImpl implements CarritoService {

    private final CatalogoService catalogo;

    @Override
    public List<CarritoItem> agregarPromocion(
            Long packId, List<CarritoItem> actuales) {

        Pack pack = catalogo.buscarPack(packId);

        if (pack == null || !Boolean.TRUE.equals(pack.getActivo())) {
            throw new IllegalArgumentException(
                    "La promoción ya no está disponible.");
        }

        if (!pack.isConStock()) {
            throw new IllegalArgumentException(
                    "La promoción no tiene existencias suficientes.");
        }

        List<CarritoItem> resultado = new ArrayList<>(actuales);
        int descuentoPack = catalogo.descuentoPack();

        for (PackDetalle detalle : pack.getDetalles()) {

            Producto producto = detalle.getProducto();
            Presentacion presentacion =
                    producto.buscarPresentacion(detalle.getMl());

            long cantidadActual = resultado.stream()
                    .filter(item ->
                            Objects.equals(item.getProductoId(), producto.getId())
                            && Objects.equals(item.getMl(), detalle.getMl()))
                    .mapToLong(CarritoItem::getCantidad)
                    .sum();

            if (cantidadActual + detalle.getCantidad()
                    > presentacion.getStock()) {

                throw new IllegalArgumentException(
                        "No hay suficientes unidades de "
                        + producto.getNombre() + " de "
                        + detalle.getMl() + " ml, considerando tu carrito.");
            }

            int descuento = Math.max(
                    descuentoPack, producto.getDescuento());

            BigDecimal precio = presentacion.getPrecio()
                    .multiply(BigDecimal.valueOf(100 - descuento))
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            CarritoItem item = new CarritoItem();
            item.setProductoId(producto.getId());
            item.setNombre(producto.getNombre());
            item.setImagenId(producto.getImagen().getId());
            item.setMl(detalle.getMl());
            item.setCantidad(detalle.getCantidad());
            item.setPrecio(precio);

            resultado.add(item);
        }

        return resultado;
    }
}