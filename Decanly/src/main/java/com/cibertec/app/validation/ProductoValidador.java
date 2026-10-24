package com.cibertec.app.validation;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cibertec.app.entity.Presentacion;
import com.cibertec.app.entity.Producto;

@Component
public class ProductoValidador {

    public void validar(Producto datos) {

        if (datos == null) {
            throw new IllegalArgumentException(
                    "No se recibió un perfume válido.");
        }

        validarDatosGenerales(datos);
        validarPresentaciones(datos.getPresentaciones());
    }

    private void validarDatosGenerales(Producto datos) {

        String nombre = datos.getNombre() == null
                ? "" : datos.getNombre().trim();

        if (nombre.isEmpty() || nombre.length() > 100) {
            throw new IllegalArgumentException(
                    "El nombre debe tener entre 1 y 100 caracteres.");
        }

        if (datos.getMarca() == null
                || datos.getMarca().getId() == null) {
            throw new IllegalArgumentException(
                    "Selecciona una marca.");
        }

        if (!"Día".equals(datos.getOcasion())
                && !"Noche".equals(datos.getOcasion())) {
            throw new IllegalArgumentException(
                    "La ocasión debe ser Día o Noche.");
        }

        String notas = datos.getNotas() == null
                ? "" : datos.getNotas().trim();

        if (notas.length() > 400) {
            throw new IllegalArgumentException(
                    "Las notas permiten hasta 400 caracteres.");
        }

        Integer descuento = datos.getDescuento();

        if (descuento == null || descuento < 0 || descuento > 70) {
            throw new IllegalArgumentException(
                    "El descuento debe estar entre 0 y 70.");
        }
    }

    private void validarPresentaciones(
            List<Presentacion> presentaciones) {

        if (presentaciones == null || presentaciones.size() != 3) {
            throw new IllegalArgumentException(
                    "Completa las presentaciones de 3, 5 y 10 ml.");
        }

        int[] tamanos = { 3, 5, 10 };

        for (int i = 0; i < tamanos.length; i++) {

            Presentacion presentacion = presentaciones.get(i);

            if (presentacion == null) {
                throw new IllegalArgumentException(
                        "Falta la presentación de "
                                + tamanos[i] + " ml.");
            }

            BigDecimal precio = presentacion.getPrecio();

            if (precio == null
                    || precio.compareTo(BigDecimal.ZERO) <= 0
                    || precio.compareTo(new BigDecimal("10000")) > 0
                    || precio.stripTrailingZeros().scale() > 2) {

                throw new IllegalArgumentException(
                        "El precio de " + tamanos[i]
                                + " ml debe ser mayor que cero, "
                                + "hasta 10000 y con máximo dos decimales.");
            }

            Integer stock = presentacion.getStock();

            if (stock == null || stock < 0 || stock > 9999) {
                throw new IllegalArgumentException(
                        "El stock de " + tamanos[i]
                                + " ml debe estar entre 0 y 9999.");
            }
        }
    }
}