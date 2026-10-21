package com.cibertec.app.controller;

import com.cibertec.app.entity.Producto;

import com.cibertec.app.service.CatalogoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

@Controller
public class TiendaController {
	@Autowired
	private CatalogoService catalogo;

	@ModelAttribute
	public void comunes(Model m) {
		m.addAttribute("marcas", catalogo.marcas());
		m.addAttribute("descuentoPack", catalogo.descuentoPack());
		m.addAttribute("tamanos", new int[] { 3, 5, 10 });
		m.addAttribute("carrito", List.of()); // TODO integrante 3: leer la sesión.
		m.addAttribute("cantidadCarrito", 0);
	}

	@GetMapping("/")
	public String inicio(Model m) {
		List<Producto> p = catalogo.listar("", "", "", "", "featured", true);
		m.addAttribute("productos", p.subList(0, Math.min(8, p.size())));
		m.addAttribute("packs", catalogo.packs());
		return "inicio/index";
	}

	@GetMapping({ "/catalogo", "/packs" })
	public String catalogo(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String marca,
			@RequestParam(defaultValue = "") String ocasion, @RequestParam(defaultValue = "") String estilo,
			@RequestParam(defaultValue = "featured") String orden, jakarta.servlet.http.HttpServletRequest request,
			Model m) {
		m.addAttribute("productos", catalogo.listar(q, marca, ocasion, estilo, orden, true));
		m.addAttribute("q", q);
		m.addAttribute("marcaElegida", marca);
		m.addAttribute("ocasion", ocasion);
		m.addAttribute("estilo", estilo);
		m.addAttribute("orden", orden);
		return request.getServletPath().equals("/packs") ? "packs/index" : "catalogo/index";
	}

	@GetMapping("/promociones")
	public String promociones(Model m) {
		m.addAttribute("packs", catalogo.packs());
		return "promociones/index";
	}

	@GetMapping("/perfume/{id}")
	public String detalle(@PathVariable Long id, Model m) {
		Producto p = catalogo.buscar(id);
		if (p == null || !p.isActivo())
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		m.addAttribute("p", p);
		m.addAttribute("galeria", catalogo.galeria(id));
		return "perfume/detalle";
	}

	@GetMapping("/perfume/detalle")
	public String detalleAnterior(@RequestParam Long id) {
		return "redirect:/perfume/" + id;
	}

	@GetMapping("/cuenta")
	public String cuenta() {
		return "cuenta/index";
	}

	@GetMapping({ "/carrito", "/carrito/vacio" })
	public String carrito() {
		return "carrito/index";
	}

	@GetMapping("/contacto")
	public String contacto() {
		return "contacto/index";
	}

	@GetMapping("/terminos")
	public String terminos() {
		return "legal/terminos";
	}

	@GetMapping("/envios")
	public String envios() {
		return "legal/envios";
	}

	@GetMapping("/privacidad")
	public String privacidad() {
		return "legal/privacidad";
	}
}
