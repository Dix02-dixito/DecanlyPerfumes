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
    public List<CarritoItem> agregarProducto(
            Long productoId,
            Integer ml,
            Integer cantidad,
            List<CarritoItem> actuales) {

        if (productoId == null || productoId <= 0
                || ml == null || (ml != 3 && ml != 5 && ml != 10)
                || cantidad == null || cantidad < 1 || cantidad > 9999) {

            throw new IllegalArgumentException(
                    "Selecciona un perfume, tamaño y cantidad válidos.");
        }

        Producto producto = catalogo.buscar(productoId);

        if (producto == null || !producto.isActivo()) {
            throw new IllegalArgumentException(
                    "El perfume ya no está disponible.");
        }

        Presentacion presentacion = producto.buscarPresentacion(ml);

        if (presentacion == null) {
            throw new IllegalArgumentException(
                    "La presentación no existe.");
        }

        long unidades = actuales.stream()
                .filter(item ->
                        Objects.equals(item.getProductoId(), productoId)
                        && Objects.equals(item.getMl(), ml))
                .mapToLong(CarritoItem::getCantidad)
                .sum();

        if (unidades + cantidad > presentacion.getStock()) {
            throw new IllegalArgumentException(
                    "No hay suficientes existencias de esta presentación.");
        }

        BigDecimal precio = presentacion.getPrecioFinal();

        CarritoItem existente = actuales.stream()
                .filter(item ->
                        item.getPackId() == null
                        && Objects.equals(item.getProductoId(), productoId)
                        && Objects.equals(item.getMl(), ml)
                        && item.getPrecio() != null
                        && item.getPrecio().compareTo(precio) == 0)
                .findFirst()
                .orElse(null);

        int nuevaCantidad = cantidad
                + (existente == null ? 0 : existente.getCantidad());

        if (nuevaCantidad > 9999) {
            throw new IllegalArgumentException(
                    "La cantidad máxima por artículo es 9999.");
        }

        CarritoItem item = new CarritoItem();
        item.setProductoId(productoId);
        item.setNombre(producto.getNombre());
        item.setImagenId(producto.getImagen().getId());
        item.setMl(ml);
        item.setCantidad(nuevaCantidad);
        item.setPrecio(precio);

        List<CarritoItem> resultado = new ArrayList<>(actuales);

        if (existente == null) {
            resultado.add(item);
        } else {
            resultado.set(resultado.indexOf(existente), item);
        }

        return resultado;
    }
    
    

    @Override
    public List<CarritoItem> agregarPromocion(
            Long packId, List<CarritoItem> actuales) {

        if (packId == null || packId <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona una promoción.");
        }

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

            long unidades = resultado.stream()
                    .filter(item ->
                            Objects.equals(item.getProductoId(), producto.getId())
                            && Objects.equals(item.getMl(), detalle.getMl()))
                    .mapToLong(CarritoItem::getCantidad)
                    .sum();

            if (unidades + detalle.getCantidad() > presentacion.getStock()) {
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
            item.setPackId(pack.getIdPack());
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