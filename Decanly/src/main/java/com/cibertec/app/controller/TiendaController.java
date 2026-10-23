package com.cibertec.app.controller;

import com.cibertec.app.entity.Producto;
import jakarta.servlet.http.HttpSession;
import com.cibertec.app.entity.CarritoItem;
import com.cibertec.app.service.CatalogoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.math.BigDecimal;
import java.util.ArrayList;

@Controller
public class TiendaController {
	@Autowired
	private CatalogoService catalogo;

	@ModelAttribute
	public void comunes(
	        Model m,
	        HttpSession session) {

	    List<?> carrito =
	            (List<?>) session.getAttribute(
	                    "carrito");

	    if (carrito == null) {
	        carrito = List.of();
	    }

	    m.addAttribute(
	            "carrito",
	            carrito);

	    m.addAttribute(
	            "cantidadCarrito",
	            carrito.size());

	    m.addAttribute(
	            "marcas",
	            catalogo.marcas());

	    m.addAttribute(
	            "descuentoPack",
	            catalogo.descuentoPack());

	    m.addAttribute(
	            "tamanos",
	            new int[]{3,5,10});
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

	@GetMapping("/carrito")
	public String carrito(
	        HttpSession session,
	        Model model) {

	    List<CarritoItem> carrito =
	            (List<CarritoItem>) session.getAttribute("carrito");

	    if (carrito == null) {
	        carrito = new ArrayList<>();
	    }

	    BigDecimal subtotal = BigDecimal.ZERO;

	    for (CarritoItem item : carrito) {
	        subtotal = subtotal.add(item.getSubtotal());
	    }

	    BigDecimal envio;
	    BigDecimal faltante;

	    if (subtotal.compareTo(new BigDecimal("200")) >= 0) {

	        envio = BigDecimal.ZERO;
	        faltante = BigDecimal.ZERO;

	    } else {

	        envio = new BigDecimal("15");

	        faltante =
	                new BigDecimal("200")
	                        .subtract(subtotal);
	    }

	    BigDecimal total =
	            subtotal.add(envio);

	    model.addAttribute("carrito", carrito);
	    model.addAttribute("subtotal", subtotal);
	    model.addAttribute("envio", envio);
	    model.addAttribute("total", total);
	    model.addAttribute("faltante", faltante);

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
