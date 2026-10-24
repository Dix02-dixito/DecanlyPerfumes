package com.cibertec.app.validation;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cibertec.app.entity.CarritoItem;

@Component
public class PedidoValidador {

    public void validar(
            String nombre,
            String telefono,
            String direccion,
            boolean aceptaTerminos,
            List<CarritoItem> carrito) {

        validarTexto(nombre, 2, 120, "El nombre");
        validarTexto(direccion, 5, 250, "La dirección");

        if (telefono == null
                || !telefono.trim().matches("\\+?[0-9]{9,15}")) {

            throw new IllegalArgumentException(
                    "Ingresa un teléfono de 9 a 15 dígitos, "
                    + "con + opcional al inicio.");
        }

        if (!aceptaTerminos) {
            throw new IllegalArgumentException(
                    "Debes aceptar los términos y condiciones.");
        }

        if (carrito == null || carrito.isEmpty()) {
            throw new IllegalArgumentException(
                    "Agrega productos al carrito antes de confirmar.");
        }

        for (CarritoItem item : carrito) {

            if (item == null
                    || item.getProductoId() == null
                    || item.getProductoId() <= 0) {

                throw new IllegalArgumentException(
                        "El carrito contiene un perfume inválido.");
            }

            Integer ml = item.getMl();

            if (ml == null || (ml != 3 && ml != 5 && ml != 10)) {
                throw new IllegalArgumentException(
                        "Selecciona una presentación de 3, 5 o 10 ml.");
            }

            Integer cantidad = item.getCantidad();

            if (cantidad == null || cantidad < 1 || cantidad > 9999) {
                throw new IllegalArgumentException(
                        "La cantidad debe estar entre 1 y 9999.");
            }

            if (item.getPackId() != null && item.getPackId() <= 0) {
                throw new IllegalArgumentException(
                        "El carrito contiene una promoción inválida.");
            }
        }
    }

    private void validarTexto(
            String valor, int minimo, int maximo, String campo) {

        if (valor == null
                || valor.trim().length() < minimo
                || valor.trim().length() > maximo) {

            throw new IllegalArgumentException(
                    campo + " debe tener entre "
                    + minimo + " y " + maximo + " caracteres.");
        }
    }
}