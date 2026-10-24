package com.cibertec.app.controller;

import com.cibertec.app.entity.Producto;
import com.cibertec.app.entity.Pack;
import com.cibertec.app.repository.PedidoRepository;
import com.cibertec.app.entity.Marca;
import com.cibertec.app.entity.Presentacion;
import com.cibertec.app.entity.PackDetalle;
import com.cibertec.app.service.PromocionService;

import com.cibertec.app.service.CatalogoService;
import com.cibertec.app.service.ProductoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.OptimisticLockException;

import java.util.List;

@Controller
public class PanelController {

	@Autowired
	private ProductoService productoService;

	@Autowired
	private CatalogoService catalogo;

	@Autowired
	private PedidoRepository pedidoRepository;
	
	@Autowired
	private PromocionService promocionService;

	// METODO EDITAR PRODUCTOS
	@InitBinder("producto")
	public void configurarFormularioProducto(WebDataBinder binder) {

		binder.setAutoGrowCollectionLimit(3);

		binder.setAllowedFields("nombre", "marca.id", "ocasion", "notas", "descuento", "version",
				"presentaciones[0].precio", "presentaciones[0].stock", "presentaciones[1].precio",
				"presentaciones[1].stock", "presentaciones[2].precio", "presentaciones[2].stock");
	}
	
	// METODO CAMBIAR ESTADO DEL PRODUCTO
	@PostMapping("/producto/{accion:delete|restore}/{id}")
	public String cambiarEstadoProducto(
	        @PathVariable("accion") String accion,
	        @PathVariable("id") Long id,
	        @RequestParam("version") Integer version,
	        RedirectAttributes avisos) {

	    boolean activar = "restore".equals(accion);

	    try {
	        productoService.cambiarEstado(id, version, activar);

	        avisos.addFlashAttribute(
	                "exito",
	                activar
	                        ? "Perfume restaurado correctamente."
	                        : "Perfume retirado correctamente.");

	    } catch (IllegalArgumentException ex) {

	        avisos.addFlashAttribute("error", ex.getMessage());

	    } catch (OptimisticLockingFailureException
	            | OptimisticLockException ex) {

	        avisos.addFlashAttribute(
	                "error",
	                "Otro usuario modificó el perfume. "
	                + "Recarga la página.");
	    }

	    return "redirect:/producto";
	}

	// METODO GUARDAR PRODUCTOS POST
	@PostMapping({ "/producto", "/producto/{id}" })
	public String guardarProducto(
	        @PathVariable(name = "id", required = false) Long id,
	        @ModelAttribute("producto") Producto producto,
	        BindingResult errores,
	        @RequestParam(name = "foto", required = false)
	        MultipartFile foto,
	        @RequestParam(name = "fotosGaleria", required = false)
	        List<MultipartFile> fotosGaleria,
	        @RequestParam(name = "eliminarImagenes", required = false)
	        List<Long> eliminarImagenes,
	        Model model,
	        RedirectAttributes avisos) {

	    if (!errores.hasErrors()) {
	        try {
	            if (id == null) {
	                productoService.guardar(
	                        producto, foto, fotosGaleria);
	            } else {
	                productoService.actualizar(
	                        id, producto, foto,
	                        fotosGaleria, eliminarImagenes);
	            }

	            avisos.addFlashAttribute(
	                    "exito",
	                    id == null
	                            ? "Perfume registrado correctamente."
	                            : "Perfume actualizado correctamente.");

	            return "redirect:/producto";

	        } catch (IllegalArgumentException ex) {
	            errores.reject("producto.datos", ex.getMessage());

	        } catch (OptimisticLockingFailureException
	                | OptimisticLockException ex) {
	            errores.reject(
	                    "producto.version",
	                    "El perfume fue modificado. Recarga la página.");
	        }
	    }

	    Producto actual = id == null ? producto : catalogo.buscar(id);

	    return mostrarFormulario(id, producto, actual, model);
	}

	//
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
		
		m.addAttribute("marcaList", catalogo.marcas());

		if (!m.containsAttribute("marca")) {
		    m.addAttribute("marca", new Marca());
		}
		
		return "producto/index";
	}
	//

	@GetMapping("/producto/new")
	public String nuevo(Model m) {
		return formulario(null, m);
	}
	//

	@GetMapping("/producto/edit/{id}")
	public String editar(@PathVariable Long id, Model m) {
		return formulario(id, m);
	}
	//

	private String formulario(Long id, Model model) {

		Producto producto = id == null ? catalogo.nuevo() : catalogo.buscar(id);

		return mostrarFormulario(id, producto, producto, model);
	}

	//
	private String mostrarFormulario(Long id, Producto datos, Producto actual, Model model) {

		if (actual == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}

		if (datos.getMarca() == null) {
			datos.setMarca(new Marca());
		}

		while (datos.getPresentaciones().size() < 3) {
			datos.getPresentaciones().add(new Presentacion());
		}

		model.addAttribute("producto", datos);
		model.addAttribute("idEdicion", id);
		model.addAttribute("marcaList", catalogo.marcas());
		model.addAttribute("tamanos", new int[] { 3, 5, 10 });

		model.addAttribute("imagenActual", actual.getImagen() == null ? null : actual.getImagen().getId());

		model.addAttribute("galeriaActual", id == null ? List.of() : catalogo.galeria(id));

		return "producto/formulario-pagina";
	}
	//

	@GetMapping("/promocion")
	public String promociones(Model m) {
	    m.addAttribute("packs", promocionService.listar());
	    return "promocion/index";
	}
	//

	@GetMapping("/promocion/new")
	public String nuevaPromocion(Model m) {
		return promocion(null, m);
	}
	//

	@GetMapping("/promocion/edit/{id}")
	public String editarPromocion(@PathVariable Long id, Model m) {
		return promocion(id, m);
	}
	
	//METODO PROMOCION
	private String promocion(Long id, Model model) {

	    Pack pack = id == null
	            ? new Pack() : catalogo.buscarPack(id);

	    return mostrarPromocion(id, pack, model);
	}

	private String mostrarPromocion(Long id, Pack pack, Model model) {

	    if (pack == null) {
	        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
	    }

	    model.addAttribute("pack", pack);
	    model.addAttribute("idEdicion", id);
	    model.addAttribute("productos",
	            catalogo.listar("", "", "", "", "featured", false));
	    model.addAttribute("tamanos", new int[] { 3, 5, 10 });

	    return "promocion/formulario";
	}
	//METODO PARA CONFIGURAR EL FORMULARIO DE PROMOCION
	@InitBinder("pack")
	public void configurarFormularioPromocion(WebDataBinder binder) {
	    binder.setAllowedFields(
	            "nombre", "descripcion", "etiqueta", "version");
	}

	@PostMapping({ "/promocion", "/promocion/{id}" })
	public String guardarPromocion(
	        @PathVariable(name = "id", required = false) Long id,
	        @ModelAttribute("pack") Pack pack,
	        BindingResult errores,
	        @RequestParam(name = "productoIds", required = false)
	        List<Long> productoIds,
	        @RequestParam(name = "ml", required = false)
	        List<Integer> ml,
	        @RequestParam(name = "cantidades", required = false)
	        List<Integer> cantidades,
	        Model model,
	        RedirectAttributes avisos) {

	    if (!errores.hasErrors()) {
	        try {
	            promocionService.guardar(
	                    id, pack, productoIds, ml, cantidades);

	            avisos.addFlashAttribute(
	                    "exito", "Promoción guardada correctamente.");

	            return "redirect:/promocion";

	        } catch (IllegalArgumentException ex) {
	            errores.reject("promocion.datos", ex.getMessage());

	        } catch (OptimisticLockingFailureException
	                | OptimisticLockException ex) {
	            errores.reject(
	                    "promocion.version",
	                    "La promoción fue modificada. Recarga la página.");
	        }
	    }

	    recuperarDetalles(pack, productoIds, ml, cantidades);

	    return mostrarPromocion(id, pack, model);
	}
	// Para recuperar los datos de presentaciones
	private void recuperarDetalles(
	        Pack pack,
	        List<Long> productoIds,
	        List<Integer> ml,
	        List<Integer> cantidades) {

	    pack.getDetalles().clear();

	    for (int i = 0; i < 3; i++) {

	        PackDetalle detalle = new PackDetalle();

	        detalle.setProductoId(
	                productoIds != null && i < productoIds.size()
	                        ? productoIds.get(i) : null);

	        detalle.setMl(
	                ml != null && i < ml.size() ? ml.get(i) : 3);

	        detalle.setCantidad(
	                cantidades != null && i < cantidades.size()
	                        ? cantidades.get(i) : 1);

	        pack.getDetalles().add(detalle);
	    }
	}
	
	//POST PARA RETIRAR Y RASTAURAR LAS PROMOCIONES
	@PostMapping("/promocion/{accion:delete|restore}/{id}")
	public String cambiarEstadoPromocion(
	        @PathVariable("accion") String accion,
	        @PathVariable("id") Long id,
	        @RequestParam("version") Integer version,
	        RedirectAttributes avisos) {

	    boolean activar = "restore".equals(accion);

	    try {
	        promocionService.cambiarEstado(id, version, activar);

	        avisos.addFlashAttribute(
	                "exito",
	                activar
	                        ? "Promoción restaurada correctamente."
	                        : "Promoción retirada correctamente.");

	    } catch (IllegalArgumentException ex) {
	        avisos.addFlashAttribute("error", ex.getMessage());

	    } catch (OptimisticLockingFailureException
	            | OptimisticLockException ex) {
	        avisos.addFlashAttribute(
	                "error",
	                "La promoción fue modificada. Recarga la página.");
	    }

	    return "redirect:/promocion";
	}
	

	@GetMapping("/pedidos")
	public String pedidos(Model model) {

		model.addAttribute("pedidos", pedidoRepository.findAll());

		return "pedido/index";
	}
	//

	@GetMapping("/usuarios")
	public String usuarios() {
		return "usuario/formulario";
	}
	
	//
	
	
}
