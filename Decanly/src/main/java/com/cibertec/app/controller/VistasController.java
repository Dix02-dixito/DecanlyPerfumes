package com.cibertec.app.controller;

import com.cibertec.app.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class VistasController {
    @Autowired private PackService packService;

    @GetMapping("/promocion")
    public String packsAdmin(Model model) {
        model.addAttribute("packs", packService.listarTodos());
        return "promocion/index";
    }

    @GetMapping("/")
    public String pagina0() {
        return "inicio/index";
    }

    @GetMapping("/catalogo")
    public String pagina1() {
        return "catalogo/index";
    }

    @GetMapping("/promociones")
    public String pagina2() {
        return "promociones/index";
    }

    @GetMapping("/packs")
    public String pagina3() {
        return "packs/index";
    }

    @GetMapping("/contacto")
    public String pagina4() {
        return "contacto/index";
    }

    @GetMapping("/cuenta")
    public String pagina5() {
        return "cuenta/index";
    }

    @GetMapping("/terminos")
    public String pagina6() {
        return "legal/terminos";
    }

    @GetMapping("/envios")
    public String pagina7() {
        return "legal/envios";
    }

    @GetMapping("/privacidad")
    public String pagina8() {
        return "legal/privacidad";
    }

    @GetMapping("/carrito")
    public String pagina9() {
        return "carrito/index";
    }

    @GetMapping("/carrito/vacio")
    public String pagina10() {
        return "carrito/vacio";
    }

    @GetMapping("/perfume/detalle")
    public String pagina11() {
        return "perfume/detalle";
    }

    @GetMapping({"/promocion/new", "/promocion/edit", "/pedidos", "/vistas"})
    public String pendiente(Model model) {
        model.addAttribute(
                "mensaje",
                "El backend de packs implementa su consulta. El alta, edición y baja de packs y el"
                    + " registro de pedidos están pendientes.");
        return "vistas/pendiente";
    }
}
