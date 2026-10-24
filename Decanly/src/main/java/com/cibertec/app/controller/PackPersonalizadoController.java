package com.cibertec.app.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.WebUtils;
import com.cibertec.app.entity.CarritoItem;
import com.cibertec.app.service.CarritoService;

import com.cibertec.app.dto.ResumenPack;
import com.cibertec.app.dto.SeleccionPack;
import com.cibertec.app.service.PackPersonalizadoService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/packs")
public class PackPersonalizadoController {

    private final PackPersonalizadoService packService;
    private final CarritoService carritoService;

    @GetMapping("/resumen")
    public ResponseEntity<ResumenPack> resumen(HttpSession session) {

        synchronized (WebUtils.getSessionMutex(session)) {

            ResumenPack resumen = packService.resumir(
                    leerSeleccion(session));

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(resumen);
        }
    }

    @PostMapping(
            value = "/{accion:agregar|quitar}/ajax",
            produces = "application/json")
    public ResponseEntity<Map<String, Object>> modificar(
            @PathVariable("accion") String accion,
            @RequestParam("productoId") Long productoId,
            @RequestParam(name = "ml", required = false) Integer ml,
            HttpSession session) {

        synchronized (WebUtils.getSessionMutex(session)) {

            try {
                List<SeleccionPack> actuales = leerSeleccion(session);

                List<SeleccionPack> resultado = "agregar".equals(accion)
                        ? packService.agregar(productoId, ml, actuales)
                        : packService.quitar(productoId, actuales);

                ResumenPack resumen = packService.resumir(resultado);

                session.setAttribute(
                        "seleccionPack", List.copyOf(resultado));

                String mensaje = "agregar".equals(accion)
                        ? "Selección del pack actualizada."
                        : "Perfume quitado del pack.";

                return ResponseEntity.ok(Map.of(
                        "mensaje", mensaje,
                        "resumen", resumen));

            } catch (IllegalArgumentException ex) {

                return ResponseEntity.badRequest().body(
                        Map.of("mensaje", ex.getMessage()));
            }
        }
    }

    private List<SeleccionPack> leerSeleccion(HttpSession session) {

        Object guardado = session.getAttribute("seleccionPack");

        return guardado instanceof List<?> lista
                ? lista.stream().map(SeleccionPack.class::cast).toList()
                : List.of();
    }
    
    @PostMapping(value = "/carrito/ajax", produces = "application/json")
    public ResponseEntity<Map<String, Object>> enviarCarrito(
            HttpSession session) {

        synchronized (WebUtils.getSessionMutex(session)) {

            try {
                Object guardado = session.getAttribute("carrito");

                List<CarritoItem> actuales = guardado instanceof List<?> lista
                        ? lista.stream().map(CarritoItem.class::cast).toList()
                        : List.of();

                List<CarritoItem> resultado =
                        carritoService.agregarPackPersonalizado(
                                leerSeleccion(session), actuales);

                ResumenPack vacio = packService.resumir(List.of());

                session.setAttribute("carrito", List.copyOf(resultado));
                session.removeAttribute("seleccionPack");

                int unidades = resultado.stream()
                        .mapToInt(CarritoItem::getCantidad)
                        .sum();

                return ResponseEntity.ok(Map.of(
                        "mensaje", "Pack añadido al carrito.",
                        "cantidad", unidades,
                        "resumen", vacio));

            } catch (IllegalArgumentException ex) {

                return ResponseEntity.badRequest().body(
                        Map.of("mensaje", ex.getMessage()));
            }
        }
    }
}