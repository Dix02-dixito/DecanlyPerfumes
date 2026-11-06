package com.cibertec.app.controller;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.math.BigDecimal;

import org.springframework.beans.InvalidPropertyException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.WebUtils;

import com.cibertec.app.dto.PedidoFormulario;
import com.cibertec.app.entity.CarritoItem;
import com.cibertec.app.entity.Pedido;
import com.cibertec.app.repository.PedidoRepository;
import com.cibertec.app.service.CatalogoService;
import com.cibertec.app.service.PedidoService;
import com.cibertec.app.service.WhatsAppService;
import com.cibertec.app.validation.PedidoValidador;
import jakarta.servlet.http.HttpSession;

@Controller
public class PedidoController {
    private final PedidoService pedidos;
    private final WhatsAppService whatsApp;
    private final PedidoRepository repositorio;
    private final CatalogoService catalogo;

    public PedidoController(PedidoService pedidos, WhatsAppService whatsApp,
            PedidoRepository repositorio, CatalogoService catalogo) {
        this.pedidos = pedidos;
        this.whatsApp = whatsApp;
        this.repositorio = repositorio;
        this.catalogo = catalogo;
    }

    @InitBinder("pedidoForm")
    public void formulario(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(50);
        binder.setAllowedFields("token", "nombreCliente", "telefono", "fechaPedido", "descuento",
                "lineas[*].productoId", "lineas[*].ml", "lineas[*].cantidad", "lineas[*].precioUnitario");
    }

    @ExceptionHandler(InvalidPropertyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public String formularioInvalido() {
        return "El formulario contiene campos inválidos. Abre Nuevo pedido e inténtalo otra vez.";
    }

    // Conserva la ruta del carrito, pero solo prepara la solicitud; no registra ni reserva stock.
    @PostMapping("/pedido/registrar")
    public String solicitar(@RequestParam String nombreCliente, @RequestParam String telefono,
            @RequestParam String direccion,
            @RequestParam(defaultValue = "false") boolean aceptaTerminos,
            HttpSession session, RedirectAttributes avisos) {
        synchronized (WebUtils.getSessionMutex(session)) {
            Object guardado = session.getAttribute("carrito");
            List<CarritoItem> carrito = guardado instanceof List<?> lista
                    ? lista.stream().map(CarritoItem.class::cast).toList() : List.of();
            try {
                Pedido solicitud = pedidos.prepararSolicitud(nombreCliente, telefono, direccion, aceptaTerminos, carrito);
                return "redirect:" + whatsApp.crearEnlace(solicitud);
            } catch (IllegalArgumentException ex) {
                avisos.addFlashAttribute("error", ex.getMessage());
                return "redirect:/carrito";
            }
        }
    }

    @GetMapping("/pedidos")
    public String listado(@RequestParam(required = false) Long id,
            @RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("pedidos", repositorio.buscar(id, q.trim()));
        model.addAttribute("idBuscado", id);
        model.addAttribute("q", q.trim());
        return "pedido/index";
    }

    @GetMapping("/pedidos/nuevo")
    public String nuevo(HttpSession session, Model model) {
        PedidoFormulario formulario = new PedidoFormulario();
        formulario.setToken(UUID.randomUUID().toString());
        formulario.setFechaPedido(LocalDate.now(PedidoValidador.ZONA_VENTA));
        formulario.getLineas().add(new PedidoFormulario.Linea());
        synchronized (WebUtils.getSessionMutex(session)) {
            Set<String> tokens = tokens(session);
            if (tokens.size() >= 20) tokens.remove(tokens.iterator().next());
            tokens.add(formulario.getToken());
        }
        model.addAttribute("pedidoForm", formulario);
        return cargarFormulario(model);
    }

    @PostMapping("/pedidos")
    public String registrar(@ModelAttribute("pedidoForm") PedidoFormulario formulario, BindingResult errores,
            @RequestParam(defaultValue = "guardar") String accion, HttpSession session,
            Model model, RedirectAttributes avisos) {
        synchronized (WebUtils.getSessionMutex(session)) {
            if (!tokens(session).contains(formulario.getToken())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El formulario venció. Abre Nuevo pedido.");
            }
            // Un doble clic, recarga o reenvío del mismo formulario lleva al pedido ya guardado.
            var existente = repositorio.findByTokenRegistro(formulario.getToken());
            if (existente.isPresent()) return "redirect:/pedidos/" + existente.get().getId();
            if ("agregar".equals(accion)) {
                if (formulario.getLineas().size() < 50) formulario.getLineas().add(new PedidoFormulario.Linea());
                return cargarFormulario(model);
            }
            if (accion.matches("quitar:[0-9]+")) {
                int indice;
                try { indice = Integer.parseInt(accion.substring(7)); }
                catch (NumberFormatException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST); }
                if (indice < formulario.getLineas().size() && formulario.getLineas().size() > 1) {
                    formulario.getLineas().remove(indice);
                }
                return cargarFormulario(model);
            }
            if (!"guardar".equals(accion)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            if (errores.hasErrors()) {
                model.addAttribute("error", "Revisa la fecha, las cantidades y los importes ingresados.");
                return cargarFormulario(model);
            }
            try {
                Long id = pedidos.registrarManual(formulario);
                avisos.addFlashAttribute("exito", "Pedido #" + id + " registrado correctamente.");
                return "redirect:/pedidos/" + id;
            } catch (IllegalArgumentException ex) {
                model.addAttribute("error", ex.getMessage());
            } catch (ConcurrencyFailureException ex) {
                model.addAttribute("error", "Las existencias cambiaron. Revisa las cantidades y vuelve a guardar.");
            }
            return cargarFormulario(model);
        }
    }

    @GetMapping("/pedidos/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Pedido pedido = pedidos.buscar(id);
        if (pedido == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        model.addAttribute("pedido", pedido);
        return "pedido/detalle";
    }

    @SuppressWarnings("unchecked")
    private Set<String> tokens(HttpSession session) {
        Set<String> tokens = (Set<String>) session.getAttribute("formulariosPedido");
        if (tokens == null) {
            tokens = new LinkedHashSet<>();
            session.setAttribute("formulariosPedido", tokens);
        }
        return tokens;
    }

    private String cargarFormulario(Model model) {
        var opciones = catalogo.listar("", "", "", "", "", true).stream()
                .map(p -> new OpcionProducto(p.getId(), p.getNombre(), p.getPresentaciones().stream()
                        .map(v -> new OpcionPresentacion(v.getMl(), v.getPrecio(), v.getStock())).toList())).toList();
        model.addAttribute("productosPedido", opciones);
        model.addAttribute("hoy", LocalDate.now(PedidoValidador.ZONA_VENTA));
        return "pedido/nuevo";
    }

    public record OpcionProducto(Long id, String nombre, List<OpcionPresentacion> presentaciones) {}
    public record OpcionPresentacion(Integer ml, BigDecimal precio, Integer stock) {}
}
