package com.cibertec.app.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cibertec.app.entity.Pedido;

@Service
public class WhatsAppService {

    private final String numero;

    public WhatsAppService(
            @Value("${decanly.whatsapp.numero}") String numero) {

        if (!numero.matches("[1-9][0-9]{7,14}")) {
            throw new IllegalArgumentException(
                    "Configura el número de WhatsApp con código de país, "
                    + "sin espacios ni el símbolo +.");
        }

        this.numero = numero;
    }

    public String crearEnlace(Pedido pedido) {

        String productos = pedido.getDetalles().stream()
                .map(detalle ->
                        "* " + detalle.getProducto().getNombre()
                        + " — " + detalle.getMl() + " ml"
                        + " × " + detalle.getCantidad())
                .collect(Collectors.joining("\n"));

        String mensaje = """
                 ¡Hola! Confirma tu pedido en Decanly

                 Pedido: #%s
                 Cliente: %s
                 WhatsApp: %s

                 Tu pedido
                %s

                 Total de productos: S/ %s
                 Envío: por coordinar con el vendedor por WhatsApp.

                 Dirección: %s

                 Confirmo que mis datos son correctos y que recibiré y pagaré el pedido contra entrega.
                """.formatted(
                        pedido.getId(),
                        pedido.getNombreCliente(),
                        pedido.getTelefono(),
                        productos,
                        pedido.getTotalPedido().setScale(2).toPlainString(),
                        pedido.getDireccion());

        return "https://wa.me/" + numero + "?text="
                + URLEncoder.encode(mensaje, StandardCharsets.UTF_8)
                        .replace("+", "%20");
    }
}