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

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.WebUtils;



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
            @RequestParam("nombreCliente") String nombreCliente,
            @RequestParam("telefono") String telefono,
            @RequestParam("direccion") String direccion,
            @RequestParam(name = "aceptaTerminos", defaultValue = "false")
            boolean aceptaTerminos,
            HttpSession session,
            RedirectAttributes avisos) {

        synchronized (WebUtils.getSessionMutex(session)) {

            Object guardado = session.getAttribute("carrito");

            List<CarritoItem> carrito = guardado instanceof List<?> lista
                    ? lista.stream().map(CarritoItem.class::cast).toList()
                    : List.of();

            try {
                Long id = pedidoService.registrar(
                        nombreCliente, telefono, direccion,
                        aceptaTerminos, carrito);

                session.removeAttribute("carrito");

                avisos.addFlashAttribute(
                        "exito", "Pedido #" + id + " registrado correctamente.");

            } catch (IllegalArgumentException ex) {

                avisos.addFlashAttribute("error", ex.getMessage());

            } catch (ConcurrencyFailureException ex) {

                avisos.addFlashAttribute(
                        "error",
                        "Las existencias cambiaron durante la compra. "
                        + "Revisa el carrito e inténtalo nuevamente.");
            }
        }

        return "redirect:/carrito";
    }
    
    @PostMapping("/pedido/{id}/cancelar")
    public String cancelar(
            @PathVariable("id") Long id,
            RedirectAttributes avisos) {

        try {
            pedidoService.cancelar(id);

            avisos.addFlashAttribute(
                    "exito", "El pedido está cancelado.");

        } catch (IllegalArgumentException ex) {

            avisos.addFlashAttribute("error", ex.getMessage());

        } catch (ConcurrencyFailureException ex) {

            avisos.addFlashAttribute(
                    "error",
                    "El pedido está siendo modificado. Inténtalo nuevamente.");
        }

        return "redirect:/pedidos";
    }
    
    @PostMapping("/pedido/{id}/estado")
    public String cambiarEstado(
            @PathVariable("id") Long id,
            @RequestParam("estado") String estado,
            RedirectAttributes avisos) {

        try {
            pedidoService.cambiarEstado(id, estado);

            avisos.addFlashAttribute(
                    "exito", "Estado del pedido actualizado correctamente.");

        } catch (IllegalArgumentException ex) {

            avisos.addFlashAttribute("error", ex.getMessage());

        } catch (ConcurrencyFailureException ex) {

            avisos.addFlashAttribute(
                    "error",
                    "El pedido está siendo modificado. Recarga e inténtalo nuevamente.");
        }

        return "redirect:/pedidos";
    }
}