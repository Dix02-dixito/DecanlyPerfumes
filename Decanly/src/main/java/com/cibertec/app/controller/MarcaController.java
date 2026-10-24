package com.cibertec.app.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cibertec.app.entity.Marca;
import com.cibertec.app.service.MarcaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/marcas")
@RequiredArgsConstructor
public class MarcaController {

    private final MarcaService marcaService;

    @GetMapping
    public String abrir(
            @RequestParam(name = "editar", required = false) Long editar,
            RedirectAttributes avisos) {

        Marca marca = editar == null
                ? new Marca() : marcaService.buscar(editar);

        if (marca == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        avisos.addFlashAttribute("marca", marca);
        return volver(avisos);
    }

    @PostMapping
    public String guardar(
            @RequestParam(name = "id", required = false) Long id,
            @RequestParam(name = "nombre", defaultValue = "") String nombre,
            RedirectAttributes avisos) {

        try {
            marcaService.guardar(id, nombre);

            avisos.addFlashAttribute(
                    "exitoMarca", "Marca guardada correctamente.");

            return volver(avisos);

        } catch (IllegalArgumentException ex) {
            avisos.addFlashAttribute("errorMarca", ex.getMessage());

        } catch (DataIntegrityViolationException ex) {
            avisos.addFlashAttribute(
                    "errorMarca", "Ya existe una marca con ese nombre.");
        }

        Marca marca = new Marca();
        marca.setId(id);
        marca.setNombre(nombre);

        avisos.addFlashAttribute("marca", marca);
        return volver(avisos);
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(
            @PathVariable("id") Long id,
            RedirectAttributes avisos) {

        try {
            marcaService.eliminar(id);

            avisos.addFlashAttribute(
                    "exitoMarca", "Marca eliminada correctamente.");

        } catch (IllegalArgumentException ex) {
            avisos.addFlashAttribute("errorMarca", ex.getMessage());

        } catch (DataIntegrityViolationException ex) {
            avisos.addFlashAttribute(
                    "errorMarca",
                    "La marca está asociada a perfumes y no puede eliminarse.");
        }

        return volver(avisos);
    }

    private String volver(RedirectAttributes avisos) {
        avisos.addFlashAttribute("abrirMarcas", true);
        return "redirect:/producto";
    }
}