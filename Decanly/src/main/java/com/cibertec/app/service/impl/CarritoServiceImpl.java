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
import java.util.UUID;

import com.cibertec.app.dto.LineaPack;
import com.cibertec.app.dto.ResumenPack;
import com.cibertec.app.dto.SeleccionPack;
import com.cibertec.app.service.PackPersonalizadoService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarritoServiceImpl implements CarritoService {

    private final CatalogoService catalogo;
    private final PackPersonalizadoService packPersonalizadoService;

    @Override
    public List<CarritoItem> modificarLinea(
            String lineaId, String accion, List<CarritoItem> actuales) {

        if (accion == null || !List.of("aumentar", "disminuir", "quitar").contains(accion)) {
            throw new IllegalArgumentException("Selecciona una acción válida.");
        }

        CarritoItem item = actuales.stream()
                .filter(linea -> linea.getLineaId().equals(lineaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "El artículo ya no está en tu carrito. Revisa tu selección."));

        if (item.getPackId() != null || item.isPackPersonalizado()) {
            if (item.getGrupoPack() == null || item.getGrupoPack().isBlank()) {
                throw new IllegalArgumentException("Vuelve a agregar este pack al carrito.");
            }
            List<CarritoItem> grupo = actuales.stream()
                    .filter(linea -> item.getGrupoPack().equals(linea.getGrupoPack()))
                    .toList();
            return switch (accion) {
                case "quitar" -> actuales.stream().filter(linea -> !grupo.contains(linea)).toList();
                case "aumentar" -> item.getPackId() != null
                        ? agregarPromocion(item.getPackId(), actuales)
                        : agregarPackPersonalizado(grupo.stream().map(linea -> new SeleccionPack(
                                linea.getProductoId(), linea.getMl(), linea.getCantidad())).toList(), actuales);
                default -> throw new IllegalArgumentException(
                        "El pack se conserva completo. Usa Quitar pack para retirarlo.");
            };
        }

        if ("quitar".equals(accion)) {
            return actuales.stream().filter(linea -> linea != item).toList();
        }

        int cantidad = item.getCantidad() + ("aumentar".equals(accion) ? 1 : -1);
        if (cantidad < 1 || cantidad > 9999) {
            throw new IllegalArgumentException("La cantidad debe estar entre 1 y 9999.");
        }

        if ("aumentar".equals(accion)) {
            Producto producto = catalogo.buscar(item.getProductoId());
            Presentacion presentacion = producto == null ? null : producto.buscarPresentacion(item.getMl());
            if (producto == null || !producto.isActivo() || presentacion == null) {
                throw new IllegalArgumentException("Esta presentación ya no está disponible.");
            }
            long unidades = actuales.stream()
                    .filter(linea -> Objects.equals(linea.getProductoId(), item.getProductoId())
                            && Objects.equals(linea.getMl(), item.getMl()))
                    .mapToLong(CarritoItem::getCantidad).sum();
            if (unidades + 1 > presentacion.getStock()) {
                throw new IllegalArgumentException("No hay suficientes existencias de esta presentación.");
            }
            if (item.getPrecio() == null || item.getPrecio().compareTo(presentacion.getPrecioFinal()) != 0) {
                throw new IllegalArgumentException("El precio cambió. Quita el artículo y vuelve a agregarlo.");
            }
        }

        // Se sustituye la línea solo después de validar; un error conserva la selección original.
        CarritoItem modificada = new CarritoItem();
        modificada.setLineaId(item.getLineaId());
        modificada.setProductoId(item.getProductoId());
        modificada.setImagenId(item.getImagenId());
        modificada.setNombre(item.getNombre());
        modificada.setMl(item.getMl());
        modificada.setCantidad(cantidad);
        modificada.setPrecio(item.getPrecio());
        List<CarritoItem> resultado = new ArrayList<>(actuales);
        resultado.set(resultado.indexOf(item), modificada);
        return List.copyOf(resultado);
    }
    
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
                        && !item.isPackPersonalizado()
                        && item.getGrupoPack() == null
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
            item.setLineaId(existente.getLineaId());
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
        String grupo = UUID.randomUUID().toString();

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
            item.setGrupoPack(grupo);
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
    
    @Override
    public List<CarritoItem> agregarPackPersonalizado(
            List<SeleccionPack> seleccion,
            List<CarritoItem> actuales) {

        ResumenPack resumen = packPersonalizadoService.resumir(seleccion);

        if (resumen.faltantes() > 0) {
            throw new IllegalArgumentException(
                    "Elige al menos tres perfumes diferentes.");
        }

        if (!resumen.listo()
                || resumen.perfumes() != resumen.items().size()) {

            throw new IllegalArgumentException(
                    "Revisa los perfumes y las existencias de tu selección.");
        }

        List<CarritoItem> resultado = new ArrayList<>(actuales);
        String grupo = UUID.randomUUID().toString();

        for (LineaPack linea : resumen.items()) {

            Producto producto = catalogo.buscar(linea.productoId());

            Presentacion presentacion = producto == null
                    ? null
                    : producto.buscarPresentacion(linea.ml());

            if (producto == null || !producto.isActivo()
                    || presentacion == null) {

                throw new IllegalArgumentException(
                        "Una presentación del pack ya no está disponible.");
            }

            long unidades = resultado.stream()
                    .filter(item ->
                            Objects.equals(
                                    item.getProductoId(), linea.productoId())
                            && Objects.equals(item.getMl(), linea.ml()))
                    .mapToLong(CarritoItem::getCantidad)
                    .sum();

            if (unidades + linea.cantidad() > presentacion.getStock()) {
                throw new IllegalArgumentException(
                        "No hay suficientes unidades de "
                        + linea.nombre() + " de " + linea.ml()
                        + " ml, considerando tu carrito.");
            }

            CarritoItem item = new CarritoItem();
            item.setProductoId(linea.productoId());
            item.setImagenId(linea.imagenId());
            item.setNombre(linea.nombre());
            item.setMl(linea.ml());
            item.setCantidad(linea.cantidad());
            item.setPrecio(linea.precio());
            item.setPackPersonalizado(true);
            item.setGrupoPack(grupo);

            resultado.add(item);
        }

        return resultado;
    }
}
