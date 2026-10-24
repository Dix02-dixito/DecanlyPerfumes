package com.cibertec.app.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.app.dto.SeleccionPack;
import com.cibertec.app.entity.Presentacion;
import com.cibertec.app.entity.Producto;
import com.cibertec.app.service.CatalogoService;
import com.cibertec.app.service.PackPersonalizadoService;
import java.math.BigDecimal;
import java.math.RoundingMode;

import com.cibertec.app.dto.LineaPack;
import com.cibertec.app.dto.ResumenPack;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PackPersonalizadoServiceImpl
        implements PackPersonalizadoService {

    private final CatalogoService catalogo;

    @Override
    public List<SeleccionPack> agregar(
            Long productoId,
            Integer ml,
            List<SeleccionPack> actuales) {

        if (productoId == null || productoId <= 0 || ml == null) {
            throw new IllegalArgumentException(
                    "Selecciona un perfume y su tamaño.");
        }

        Producto producto = catalogo.buscar(productoId);

        if (producto == null || !producto.isActivo()) {
            throw new IllegalArgumentException(
                    "Este perfume ya no está disponible.");
        }

        List<SeleccionPack> resultado = new ArrayList<>(actuales);

        int indice = -1;

        for (int i = 0; i < resultado.size(); i++) {
            if (Objects.equals(
                    resultado.get(i).productoId(), productoId)) {

                indice = i;
                break;
            }
        }

        int cantidad = indice < 0
                ? 1
                : resultado.get(indice).cantidad();

        Presentacion presentacion = producto.buscarPresentacion(ml);

        if (presentacion == null
                || presentacion.getStock() < cantidad) {

            throw new IllegalArgumentException(
                    "Selecciona una presentación con existencias suficientes.");
        }

        SeleccionPack seleccion = new SeleccionPack(
                productoId, ml, cantidad);

        if (indice < 0) {
            resultado.add(seleccion);
        } else {
            resultado.set(indice, seleccion);
        }

        return resultado;
    }

    @Override
    public List<SeleccionPack> quitar(
            Long productoId,
            List<SeleccionPack> actuales) {

        if (productoId == null || productoId <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona el perfume que deseas quitar.");
        }

        return actuales.stream()
                .filter(item ->
                        !Objects.equals(item.productoId(), productoId))
                .toList();
    }
    
    @Override
    public ResumenPack resumir(List<SeleccionPack> seleccion) {

        int perfumes = (int) seleccion.stream()
                .map(SeleccionPack::productoId)
                .distinct()
                .count();

        int unidades = seleccion.stream()
                .mapToInt(SeleccionPack::cantidad)
                .sum();

        int descuentoPack = perfumes >= 3
                ? catalogo.descuentoPack()
                : 0;

        List<LineaPack> lineas = new ArrayList<>();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        boolean conErrores = false;

        for (SeleccionPack item : seleccion) {

            Producto producto = catalogo.buscar(item.productoId());

            Presentacion presentacion = producto == null
                    ? null
                    : producto.buscarPresentacion(item.ml());

            String nombre = producto == null
                    ? "Perfume no disponible"
                    : producto.getNombre();

            Long imagenId = producto == null || producto.getImagen() == null
                    ? null
                    : producto.getImagen().getId();

            String error = "";

            if (producto == null || !producto.isActivo()) {
                error = "Este perfume ya no está disponible.";
            } else if (presentacion == null) {
                error = "Esta presentación ya no existe.";
            } else if (presentacion.getStock() < item.cantidad()) {
                error = "No hay existencias suficientes.";
            }

            if (!error.isEmpty()) {

                conErrores = true;

                lineas.add(new LineaPack(
                        item.productoId(), imagenId, nombre,
                        item.ml(), item.cantidad(),
                        null, null, error));

                continue;
            }

            int descuento = Math.max(
                    producto.getDescuento(), descuentoPack);

            BigDecimal precio = presentacion.getPrecio()
                    .multiply(BigDecimal.valueOf(100 - descuento))
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            BigDecimal cantidad = BigDecimal.valueOf(item.cantidad());
            BigDecimal importe = precio.multiply(cantidad);

            subtotal = subtotal.add(
                    presentacion.getPrecio().multiply(cantidad));

            total = total.add(importe);

            lineas.add(new LineaPack(
                    item.productoId(), imagenId, nombre,
                    item.ml(), item.cantidad(),
                    precio, importe, ""));
        }

        return new ResumenPack(
                List.copyOf(lineas),
                conErrores ? null : subtotal,
                conErrores ? null : subtotal.subtract(total),
                conErrores ? null : total,
                perfumes,
                unidades,
                Math.max(0, 3 - perfumes),
                perfumes >= 3 && !conErrores);
    }
}