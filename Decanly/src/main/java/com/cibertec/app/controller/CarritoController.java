package com.cibertec.app.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.cibertec.app.service.CarritoService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cibertec.app.entity.CarritoItem;
import com.cibertec.app.entity.Presentacion;
import com.cibertec.app.entity.Producto;
import com.cibertec.app.service.CatalogoService;

import jakarta.servlet.http.HttpSession;

@Controller
public class CarritoController {

    private final CatalogoService catalogo;
    private final CarritoService carritoService;

    public CarritoController(
            CatalogoService catalogo,
            CarritoService carritoService) {

        this.catalogo = catalogo;
        this.carritoService = carritoService;
    }

    @PostMapping("/carrito/agregar")
    public String agregar(
            @RequestParam Long productoId,
            @RequestParam Integer ml,
            @RequestParam Integer cantidad,
            HttpSession session) {

        Producto producto =
                catalogo.buscar(productoId);

        if (producto == null) {
            return "redirect:/catalogo";
        }

        Presentacion presentacion =
                producto.buscarPresentacion(ml);

        if (presentacion == null) {
            return "redirect:/catalogo";
        }

        List<CarritoItem> carrito =
                (List<CarritoItem>)
                        session.getAttribute("carrito");

        if (carrito == null) {
            carrito = new ArrayList<>();
        }

        CarritoItem item =
                new CarritoItem();

        item.setProductoId(productoId);
        item.setNombre(
                producto.getNombre());
        item.setImagenId(
        	    producto.getImagen().getId()
        	);
        item.setMl(ml);
        item.setCantidad(cantidad);
        item.setPrecio(
                presentacion.getPrecioFinal());

        carrito.add(item);

        session.setAttribute(
                "carrito",
                carrito);

        return "redirect:/carrito";
    }
    
    @PostMapping("/carrito/vaciar")
    public String vaciar(HttpSession session) {

        session.removeAttribute("carrito");

        return "redirect:/carrito";
    }
    
    @PostMapping("/carrito/promocion")
    public String agregarPromocion(
            @RequestParam("packId") Long packId,
            @RequestParam(name = "destino", defaultValue = "carrito") String destino,
            HttpSession session,
            RedirectAttributes avisos) {

        Object guardado = session.getAttribute("carrito");

        List<CarritoItem> actuales = guardado instanceof List<?> lista
                ? lista.stream().map(CarritoItem.class::cast).toList()
                : List.of();

        try {
            List<CarritoItem> resultado =
                    carritoService.agregarPromocion(packId, actuales);

            session.setAttribute("carrito", resultado);

            if ("comprar".equals(destino)) {
                return "redirect:/carrito#datos-pedido";
            }

            avisos.addFlashAttribute(
                    "exito", "Promoción agregada al carrito.");

            return "redirect:/promociones";

        } catch (IllegalArgumentException ex) {
            avisos.addFlashAttribute("error", ex.getMessage());
            return "redirect:/promociones";
        }
    }
}
