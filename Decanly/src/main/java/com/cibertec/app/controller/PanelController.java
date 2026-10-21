package com.cibertec.app.controller;

import com.cibertec.app.entity.Producto;
import com.cibertec.app.entity.Pack;

import com.cibertec.app.service.CatalogoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

@Controller
public class PanelController {
	@Autowired
	private CatalogoService catalogo;

	@GetMapping("/producto")
	public String productos(@RequestParam(defaultValue = "") String q, Model m) {
		List<Producto> todos = catalogo.listar("", "", "", "", "featured", false);
		int stock = 0;
		for (Producto p : todos)
			if (p.isActivo() && p.isConStock())
				stock++;
		m.addAttribute("productos", catalogo.listar(q, "", "", "", "featured", false));
		m.addAttribute("totalProductos", todos.size());
		m.addAttribute("productosConStock", stock);
		m.addAttribute("q", q);
		return "producto/index";
	}

	@GetMapping("/producto/new")
	public String nuevo(Model m) {
		return formulario(null, m);
	}

	@GetMapping("/producto/edit/{id}")
	public String editar(@PathVariable Long id, Model m) {
		return formulario(id, m);
	}

	private String formulario(Long id, Model m) {
		Producto p = id == null ? catalogo.nuevo() : catalogo.buscar(id);
		if (p == null)
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		m.addAttribute("producto", p);
		m.addAttribute("idEdicion", id);
		m.addAttribute("marcaList", catalogo.marcas());
		m.addAttribute("tamanos", new int[] { 3, 5, 10 });
		m.addAttribute("imagenActual", id == null ? null : p.getImagen().getId());
		m.addAttribute("galeriaActual", id == null ? List.of() : catalogo.galeria(id));
		return "producto/formulario-pagina";
	}

	@GetMapping("/promocion")
	public String promociones(Model m) {
		m.addAttribute("packs", catalogo.packs());
		return "promocion/index";
	}

	@GetMapping("/promocion/new")
	public String nuevaPromocion(Model m) {
		return promocion(null, m);
	}

	@GetMapping("/promocion/edit/{id}")
	public String editarPromocion(@PathVariable Long id, Model m) {
		return promocion(id, m);
	}

	private String promocion(Long id, Model m) {
		Pack p = id == null ? new Pack() : catalogo.buscarPack(id);
		if (p == null)
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		m.addAttribute("pack", p);
		m.addAttribute("idEdicion", id);
		m.addAttribute("productos", catalogo.listar("", "", "", "", "featured", false));
		m.addAttribute("tamanos", new int[] { 3, 5, 10 });
		return "promocion/formulario";
	}

	@GetMapping("/pedidos")
	public String pedidos() {
		return "pedido/index";
	}

	@GetMapping("/usuarios")
	public String usuarios() {
		return "usuario/formulario";
	}
}
