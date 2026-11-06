package com.cibertec.app.validation;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import com.cibertec.app.dto.PedidoFormulario;

import org.springframework.stereotype.Component;

import com.cibertec.app.entity.CarritoItem;

@Component
public class PedidoValidador {

    public static final ZoneId ZONA_VENTA = ZoneId.of("America/Lima");
    public static final BigDecimal MAXIMO = new BigDecimal("99999999.99");

    public void validarManual(PedidoFormulario formulario) {
        validarTexto(formulario.getNombreCliente(), 2, 120, "El nombre");
        validarTelefono(formulario.getTelefono());
        if (formulario.getToken() == null || !formulario.getToken().matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            throw new IllegalArgumentException("Abre un formulario de nuevo pedido para registrar la venta.");
        }
        if (formulario.getFechaPedido() == null || formulario.getFechaPedido().getYear() < 1000
                || formulario.getFechaPedido().isAfter(LocalDate.now(ZONA_VENTA))) {
            throw new IllegalArgumentException("Ingresa la fecha real de la venta, sin fechas futuras.");
        }
        validarImporte(formulario.getDescuento(), "El descuento", true);
        if (formulario.getLineas() == null || formulario.getLineas().isEmpty() || formulario.getLineas().size() > 50) {
            throw new IllegalArgumentException("Agrega entre 1 y 50 líneas de productos.");
        }
        for (var linea : formulario.getLineas()) {
            if (linea == null || linea.getProductoId() == null || linea.getProductoId() < 1) {
                throw new IllegalArgumentException("Selecciona un perfume en cada línea.");
            }
            if (linea.getMl() == null || !List.of(3, 5, 10).contains(linea.getMl())) {
                throw new IllegalArgumentException("Selecciona una presentación de 3, 5 o 10 ml.");
            }
            if (linea.getCantidad() == null || linea.getCantidad() < 1 || linea.getCantidad() > 9999) {
                throw new IllegalArgumentException("La cantidad debe estar entre 1 y 9999.");
            }
            validarImporte(linea.getPrecioUnitario(), "El precio unitario", false);
        }
    }

    private void validarImporte(BigDecimal importe, String campo, boolean permiteCero) {
        if (importe == null || importe.signum() < 0 || (!permiteCero && importe.signum() == 0)
                || importe.compareTo(MAXIMO) > 0 || importe.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(campo + " debe ser un importe válido en soles con hasta dos decimales.");
        }
    }

    private void validarTelefono(String telefono) {
        if (telefono == null || !telefono.trim().matches("\\+?[0-9]{9,15}")) {
            throw new IllegalArgumentException("Ingresa un teléfono de 9 a 15 dígitos, con + opcional al inicio.");
        }
    }

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
