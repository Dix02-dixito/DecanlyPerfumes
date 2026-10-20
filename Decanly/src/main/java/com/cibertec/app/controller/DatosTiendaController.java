package com.cibertec.app.controller;

import com.cibertec.app.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@ControllerAdvice(assignableTypes = {VistasController.class, ProductoController.class})
public class DatosTiendaController {
    @Autowired private CatalogoService catalogoService;

    @Value("${decanly.seller-phone}")
    private String telefono;

    @ModelAttribute("datosTienda")
    public Map<String, Object> datosTienda(Authentication auth) {
        Map<String, Object> datos = new LinkedHashMap<>(catalogoService.obtenerDatosTienda());
        boolean admin =
                auth != null
                        && auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        datos.put(
                "user",
                auth == null
                        ? null
                        : Map.of("email", auth.getName(), "role", admin ? "ADMIN" : "LECTOR"));
        datos.put("sellerPhone", telefono);
        return datos;
    }
}
