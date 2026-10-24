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
import java.util.Objects;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.HttpSession;

@Controller
public class CarritoController {

	private final CarritoService carritoService;

	public CarritoController(CarritoService carritoService) {
	    this.carritoService = carritoService;
	}

	@PostMapping("/carrito/{tipo:agregar|promocion}")
	public String agregar(
	        @PathVariable("tipo") String tipo,
	        @RequestParam(name = "productoId", required = false) Long productoId,
	        @RequestParam(name = "ml", required = false) Integer ml,
	        @RequestParam(name = "cantidad", defaultValue = "1") Integer cantidad,
	        @RequestParam(name = "packId", required = false) Long packId,
	        @RequestParam(name = "destino", defaultValue = "carrito") String destino,
	        HttpSession session,
	        RedirectAttributes avisos) {

	    try {
	        actualizarCarrito(tipo, productoId, ml, cantidad, packId, session);

	        if ("comprar".equals(destino)) {
	            return "redirect:/carrito#datos-pedido";
	        }

	        avisos.addFlashAttribute(
	                "exito", "Selección añadida al carrito.");

	    } catch (IllegalArgumentException ex) {
	        avisos.addFlashAttribute("error", ex.getMessage());
	    }

	    return "promocion".equals(tipo)
	            ? "redirect:/promociones"
	            : "redirect:/catalogo";
	}

	@PostMapping("/carrito/vaciar")
	public String vaciar(HttpSession session) {

		session.removeAttribute("carrito");

		return "redirect:/carrito";
	}

	@PostMapping(
	        value = "/carrito/{tipo:agregar|promocion}/ajax",
	        produces = "application/json")
	public ResponseEntity<Map<String, Object>> agregarAjax(
	        @PathVariable("tipo") String tipo,
	        @RequestParam(name = "productoId", required = false) Long productoId,
	        @RequestParam(name = "ml", required = false) Integer ml,
	        @RequestParam(name = "cantidad", defaultValue = "1") Integer cantidad,
	        @RequestParam(name = "packId", required = false) Long packId,
	        HttpSession session) {

	    try {
	        int unidades = actualizarCarrito(
	                tipo, productoId, ml, cantidad, packId, session);

	        String mensaje = "promocion".equals(tipo)
	                ? "Promoción añadida al carrito."
	                : "Producto añadido al carrito.";

	        return ResponseEntity.ok(Map.of(
	                "mensaje", mensaje,
	                "cantidad", unidades));

	    } catch (IllegalArgumentException ex) {
	        return ResponseEntity.badRequest().body(
	                Map.of("mensaje", ex.getMessage()));
	    }
	}
	
	private int actualizarCarrito(
	        String tipo,
	        Long productoId,
	        Integer ml,
	        Integer cantidad,
	        Long packId,
	        HttpSession session) {

	    synchronized (WebUtils.getSessionMutex(session)) {

	        Object guardado = session.getAttribute("carrito");

	        List<CarritoItem> actuales = guardado instanceof List<?> lista
	                ? lista.stream().map(CarritoItem.class::cast).toList()
	                : List.of();

	        if ("promocion".equals(tipo) && packId == null) {
	            throw new IllegalArgumentException(
	                    "Selecciona una promoción.");
	        }

	        List<CarritoItem> resultado = "promocion".equals(tipo)
	                ? carritoService.agregarPromocion(packId, actuales)
	                : carritoService.agregarProducto(
	                        productoId, ml, cantidad, actuales);

	        session.setAttribute("carrito", resultado);

	        return resultado.stream()
	                .mapToInt(CarritoItem::getCantidad)
	                .sum();
	    }
	}
}
