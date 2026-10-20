package com.cibertec.app.controller;

import com.cibertec.app.entity.Producto;
import com.cibertec.app.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class ProductoController {
    @Autowired private ProductoService productoService;
    @Autowired private MarcaService marcaService;

    @InitBinder("producto")
    public void limitarCampos(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(3);
        binder.setAllowedFields(
                "nombre",
                "marca.id",
                "ocasion",
                "notas",
                "descuento",
                "version",
                "presentaciones[0].precio",
                "presentaciones[0].stock",
                "presentaciones[1].precio",
                "presentaciones[1].stock",
                "presentaciones[2].precio",
                "presentaciones[2].stock");
    }

    @GetMapping("/producto")
    public String listProductos(@RequestParam(defaultValue = "") String q, Model model) {
        String busqueda = q.trim().toLowerCase(Locale.ROOT);
        List<Producto> todos = productoService.listarTodosProductos();
        model.addAttribute(
                "productos",
                todos.stream()
                        .filter(
                                v ->
                                        (v.getNombre() + " " + v.getMarca().getNombre())
                                                .toLowerCase(Locale.ROOT)
                                                .contains(busqueda))
                        .toList());
        model.addAttribute("totalProductos", todos.size());
        model.addAttribute(
                "productosConStock",
                todos.stream().filter(v -> v.isActivo() && v.isConStock()).count());
        model.addAttribute("q", q);
        return "producto/index";
    }

    @GetMapping("/producto/new")
    public String createProducto(Model model) {
        model.addAttribute("producto", productoService.nuevoProducto());
        prepararFormulario(model, null);
        return "producto/create";
    }

    @PostMapping("/producto")
    public String saveProducto(
            @ModelAttribute("producto") Producto producto,
            BindingResult result,
            @RequestParam(required = false) MultipartFile foto,
            @RequestParam(required = false) List<MultipartFile> fotosGaleria,
            Model model,
            RedirectAttributes redirect) {
        if (!result.hasErrors())
            try {
                productoService.guardarProducto(producto, foto, fotosGaleria);
                redirect.addFlashAttribute("exito", "Perfume guardado en MySQL.");
                return "redirect:/producto";
            } catch (IllegalArgumentException ex) {
                result.reject("producto", ex.getMessage());
            }
        prepararFormulario(model, null);
        return "producto/create";
    }

    @GetMapping("/producto/edit/{id}")
    public String editProducto(@PathVariable Long id, Model model) {
        Producto v = productoService.buscarProductoById(id);
        if (v == null)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND);
        model.addAttribute("producto", v);
        prepararFormulario(model, id);
        return "producto/edit";
    }

    @GetMapping("/producto/edit")
    public String editAnterior(@RequestParam Long id) {
        return "redirect:/producto/edit/" + id;
    }

    @PostMapping("/producto/{id}")
    public String updateProducto(
            @PathVariable Long id,
            @ModelAttribute("producto") Producto producto,
            BindingResult result,
            @RequestParam(required = false) MultipartFile foto,
            @RequestParam(required = false) List<MultipartFile> fotosGaleria,
            @RequestParam(required = false) List<Long> eliminarImagenes,
            Model model,
            RedirectAttributes redirect) {
        if (!result.hasErrors())
            try {
                productoService.actualizarProducto(
                        id, producto, foto, fotosGaleria, eliminarImagenes);
                redirect.addFlashAttribute("exito", "Perfume actualizado en MySQL.");
                return "redirect:/producto";
            } catch (IllegalArgumentException ex) {
                result.reject("producto", ex.getMessage());
            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
                result.reject(
                        "version", "Otra persona modificó este perfume. Vuelve a abrir Editar.");
            }
        prepararFormulario(model, id);
        return "producto/edit";
    }

    private void prepararFormulario(Model model, Long id) {
        model.addAttribute("idEdicion", id);
        model.addAttribute("marcaList", marcaService.listarTodosMarca());
        model.addAttribute(
                "galeriaActual", id == null ? List.of() : productoService.listarGaleria(id));
        Producto actual = id == null ? null : productoService.buscarProductoById(id);
        model.addAttribute("imagenActual", actual == null ? null : actual.getImagen().getId());
        model.addAttribute("tamanos", new int[] {3, 5, 10});
    }

    @PostMapping("/producto/delete/{id}")
    public String deleteProducto(
            @PathVariable Long id, @RequestParam Integer version, RedirectAttributes redirect) {
        productoService.eliminarProductoById(id, version);
        redirect.addFlashAttribute("exito", "Perfume retirado. Puedes restaurarlo.");
        return "redirect:/producto";
    }

    @PostMapping("/producto/restore/{id}")
    public String restoreProducto(
            @PathVariable Long id, @RequestParam Integer version, RedirectAttributes redirect) {
        productoService.restaurarProductoById(id, version);
        redirect.addFlashAttribute("exito", "Perfume publicado nuevamente.");
        return "redirect:/producto";
    }

    @PostMapping("/producto/agotado/{id}")
    public String agotarProducto(
            @PathVariable Long id, @RequestParam Integer version, RedirectAttributes redirect) {
        productoService.agotarProductoById(id, version);
        redirect.addFlashAttribute("exito", "Stock de los tres tamaños en cero.");
        return "redirect:/producto";
    }
}
