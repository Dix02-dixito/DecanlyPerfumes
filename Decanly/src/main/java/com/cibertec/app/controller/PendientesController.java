package com.cibertec.app.controller;

import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.PostMapping;
import jakarta.servlet.http.HttpServletResponse;

// Sustituir cada ruta por su controlador al implementar el módulo asignado.
// 501 hace explícito que NO se guardó ni se autenticó nada.
@Controller
public class PendientesController {
	@PostMapping({ "/producto", "/producto/{id}", "/producto/delete/{id}", "/producto/restore/{id}",
			"/producto/agotado/{id}", "/promocion", "/promocion/{id}", "/promocion/delete/{id}", "/carrito/agregar",
			"/carrito/quitar", "/carrito/cantidad", "/carrito/vaciar", "/packs/agregar", "/packs/quitar",
			"/packs/carrito", "/promociones/agregar", "/pedido/registrar", "/pedido/{id}/estado",
			"/pedido/{id}/cancelar", "/login", "/logout", "/usuarios", "/configuracion" })
	public String pendiente(HttpServletResponse response) {
		response.setStatus(501);
		return "vistas/pendiente";
	}
}
