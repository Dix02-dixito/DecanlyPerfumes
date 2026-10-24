package com.cibertec.app.validation;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cibertec.app.entity.Pack;

@Component
public class PromocionValidador {

    public void validar(
            Pack datos,
            List<Long> productoIds,
            List<Integer> ml,
            List<Integer> cantidades) {

        if (datos == null) {
            throw new IllegalArgumentException(
                    "No se recibieron los datos de la promoción.");
        }

        validarTexto(datos.getNombre(), 60, "El título");
        validarTexto(datos.getDescripcion(), 180, "La descripción");

        if (datos.getEtiqueta() != null
                && datos.getEtiqueta().trim().length() > 40) {
            throw new IllegalArgumentException(
                    "La etiqueta permite hasta 40 caracteres.");
        }

        if (productoIds == null || productoIds.size() != 3
                || ml == null || ml.size() != 3
                || cantidades == null || cantidades.size() != 3) {
            throw new IllegalArgumentException(
                    "Completa los tres perfumes del pack.");
        }

        HashSet<Long> seleccionados = new HashSet<>();

        for (int i = 0; i < 3; i++) {

            Long productoId = productoIds.get(i);

            if (productoId == null || productoId <= 0
                    || !seleccionados.add(productoId)) {
                throw new IllegalArgumentException(
                        "Selecciona tres perfumes diferentes.");
            }

            Integer tamano = ml.get(i);

            if (tamano == null
                    || (tamano != 3 && tamano != 5 && tamano != 10)) {
                throw new IllegalArgumentException(
                        "Los tamaños permitidos son 3, 5 y 10 ml.");
            }

            Integer cantidad = cantidades.get(i);

            if (cantidad == null || cantidad < 1 || cantidad > 10) {
                throw new IllegalArgumentException(
                        "Cada cantidad debe estar entre 1 y 10.");
            }
        }
    }

    private void validarTexto(String valor, int maximo, String campo) {

        if (valor == null || valor.isBlank()
                || valor.trim().length() > maximo) {
            throw new IllegalArgumentException(
                    campo + " es obligatorio y permite hasta "
                            + maximo + " caracteres.");
        }
    }
}