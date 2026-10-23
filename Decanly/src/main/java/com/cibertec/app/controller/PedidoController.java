package com.cibertec.app.controller;
import java.util.ArrayList;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.servlet.http.HttpSession;
import com.cibertec.app.entity.CarritoItem;
import java.math.BigDecimal;
import java.util.List;



import com.cibertec.app.service.PedidoService;

@Controller
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(
            PedidoService pedidoService) {

        this.pedidoService =
                pedidoService;
    }

    @PostMapping("/pedido/registrar")
    public String registrar(
            @RequestParam String nombreCliente,
            @RequestParam String telefono,
            @RequestParam String direccion,
            HttpSession session) {

        List<CarritoItem> carrito =
                (List<CarritoItem>) session.getAttribute("carrito");

        BigDecimal total = BigDecimal.ZERO;

        if (carrito != null) {

            for (CarritoItem item : carrito) {
                total = total.add(item.getSubtotal());
            }

            if (total.compareTo(new BigDecimal("200")) < 0) {
                total = total.add(new BigDecimal("15"));
            }
        }

        pedidoService.registrar(
                nombreCliente,
                telefono,
                direccion,
                total);

        return "redirect:/pedidos";
    }
    
    @PostMapping("/pedido/{id}/cancelar")
    public String cancelar(
            @PathVariable Long id) {

        pedidoService.cancelar(id);

        return "redirect:/pedidos";
    }
}