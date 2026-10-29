package com.cibertec.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.cibertec.app.controller.CarritoController;
import com.cibertec.app.controller.TiendaController;
import com.cibertec.app.dto.SeleccionPack;
import com.cibertec.app.entity.*;
import com.cibertec.app.repository.*;
import com.cibertec.app.service.impl.*;
import com.cibertec.app.validation.PedidoValidador;

class CarritoEdicionTest {
    CatalogoService catalogo;
    CarritoServiceImpl carrito;
    Map<Long, Producto> productos;
    Pack promocion;

    @BeforeEach
    void prepararCatalogo() {
        catalogo = mock(CatalogoService.class);
        productos = new HashMap<>();
        for (long id = 1; id <= 3; id++) {
            Producto producto = new Producto();
            producto.setId(id);
            producto.setNombre("Perfume " + id);
            Imagen imagen = new Imagen();
            imagen.setId(id);
            producto.setImagen(imagen);
            for (int ml : new int[] {3, 5, 10}) {
                Presentacion presentacion = new Presentacion();
                presentacion.setProducto(producto);
                presentacion.setMl(ml);
                presentacion.setPrecio(new BigDecimal("39.00"));
                presentacion.setStock(20);
                producto.getPresentaciones().add(presentacion);
            }
            productos.put(id, producto);
            when(catalogo.buscar(id)).thenReturn(producto);
        }
        when(catalogo.descuentoPack()).thenReturn(10);
        promocion = new Pack();
        promocion.setIdPack(9L);
        for (long id = 1; id <= 3; id++) {
            PackDetalle d = new PackDetalle();
            d.setPackId(9L);
            d.setProductoId(id);
            d.setProducto(productos.get(id));
            d.setMl(5);
            d.setCantidad(id == 1 ? 2 : 1);
            promocion.getDetalles().add(d);
        }
        when(catalogo.buscarPack(9L)).thenReturn(promocion);
        carrito = new CarritoServiceImpl(catalogo, new PackPersonalizadoServiceImpl(catalogo));
    }

    @Test
    void cantidadesEditadasLleganAlTotalDetalleStockYWhatsApp() {
        List<CarritoItem> seleccion = carrito.agregarProducto(1L, 5, 1, List.of());
        String id = seleccion.get(0).getLineaId();
        seleccion = carrito.modificarLinea(id, "aumentar", seleccion);
        seleccion = carrito.modificarLinea(id, "aumentar", seleccion);
        seleccion = carrito.modificarLinea(id, "disminuir", seleccion);
        seleccion = carrito.agregarProducto(1L, 3, 1, seleccion);
        seleccion = carrito.modificarLinea(seleccion.get(1).getLineaId(), "quitar", seleccion);
        assertEquals(1, seleccion.size());
        assertEquals(2, seleccion.get(0).getCantidad());
        assertEquals(id, seleccion.get(0).getLineaId());
        var session = new MockHttpSession();
        session.setAttribute("carrito", seleccion);
        var model = new ExtendedModelMap();
        new TiendaController().carrito(session, model);
        assertEquals(new BigDecimal("78.00"), model.get("total"));
        Pedido pedido = registrar(seleccion);
        assertEquals(1, pedido.getDetalles().size());
        assertEquals(2, pedido.getDetalles().get(0).getCantidad());
        assertEquals(5, pedido.getDetalles().get(0).getMl());
        assertEquals(new BigDecimal("78.00"), pedido.getTotalPedido());
        assertEquals(18, productos.get(1L).buscarPresentacion(5).getStock());
        assertEquals(20, productos.get(1L).buscarPresentacion(3).getStock());
        String mensaje = URLDecoder.decode(new WhatsAppService("51907032488").crearEnlace(pedido), StandardCharsets.UTF_8);
        assertTrue(mensaje.contains("Perfume 1 — 5 ml × 2"));
        assertTrue(mensaje.contains("S/ 78.00"));
    }

    @Test
    void aumentarCuentaExistenciasDeIndividualesYPromocionesSinMutarAlFallar() {
        List<CarritoItem> seleccion = carrito.agregarProducto(1L, 5, 1, List.of());
        seleccion = carrito.agregarPromocion(9L, seleccion);
        productos.get(1L).buscarPresentacion(5).setStock(3);
        List<CarritoItem> original = seleccion;
        assertThrows(IllegalArgumentException.class, () -> carrito.modificarLinea(original.get(0).getLineaId(), "aumentar", original));
        assertEquals(1, original.get(0).getCantidad());
        assertEquals(4, original.size());
    }

    @Test
    void repetirUnPackSinExistenciasNoAgregaComponentesParciales() {
        List<CarritoItem> original = carrito.agregarPromocion(9L, List.of());
        productos.get(3L).buscarPresentacion(5).setStock(1);
        assertThrows(IllegalArgumentException.class, () -> carrito.modificarLinea(original.get(0).getLineaId(), "aumentar", original));
        assertEquals(3, original.size());
        assertEquals(List.of(2, 1, 1), original.stream().map(CarritoItem::getCantidad).toList());
    }

    @Test
    void precioCambiadoNoPermiteAumentarPeroSiDisminuirYQuitar() {
        List<CarritoItem> original = carrito.agregarProducto(1L, 5, 2, List.of());
        productos.get(1L).buscarPresentacion(5).setPrecio(new BigDecimal("45.00"));
        assertThrows(IllegalArgumentException.class, () -> carrito.modificarLinea(original.get(0).getLineaId(), "aumentar", original));
        List<CarritoItem> menos = carrito.modificarLinea(original.get(0).getLineaId(), "disminuir", original);
        assertEquals(2, original.get(0).getCantidad());
        assertEquals(1, menos.get(0).getCantidad());
        assertEquals(new BigDecimal("39.00"), menos.get(0).getPrecio());
        assertTrue(carrito.modificarLinea(menos.get(0).getLineaId(), "quitar", menos).isEmpty());
    }

    @Test
    void perfumeDesactivadoSePuedeQuitarSinAfectarOtrosTamanos() {
        List<CarritoItem> original = carrito.agregarProducto(1L, 5, 1, List.of());
        original = carrito.agregarProducto(1L, 3, 2, original);
        productos.get(1L).setActivo(false);
        List<CarritoItem> seleccion = original;
        assertThrows(IllegalArgumentException.class, () -> carrito.modificarLinea(seleccion.get(0).getLineaId(), "aumentar", seleccion));
        List<CarritoItem> restante = carrito.modificarLinea(seleccion.get(0).getLineaId(), "quitar", seleccion);
        assertEquals(1, restante.size());
        assertEquals(3, restante.get(0).getMl());
        assertEquals(2, restante.get(0).getCantidad());
    }

    @Test
    void limitesImpidenCantidadCeroYDesbordes() {
        List<CarritoItem> original = carrito.agregarProducto(1L, 5, 1, List.of());
        assertThrows(IllegalArgumentException.class, () -> carrito.modificarLinea(original.get(0).getLineaId(), "disminuir", original));
        original.get(0).setCantidad(9999);
        assertThrows(IllegalArgumentException.class, () -> carrito.modificarLinea(original.get(0).getLineaId(), "aumentar", original));
        assertEquals(9999, original.get(0).getCantidad());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "actualizar", "-1", "eliminar-todo"})
    void accionesDesconocidasNoCambianElCarrito(String accion) {
        List<CarritoItem> original = carrito.agregarProducto(1L, 5, 1, List.of());
        assertThrows(IllegalArgumentException.class, () -> carrito.modificarLinea(original.get(0).getLineaId(), accion, original));
        assertEquals(1, original.get(0).getCantidad());
    }

    @Test
    void unaPaginaAntiguaNoQuitaOtroArticuloNiRecreaUnoEliminado() {
        List<CarritoItem> original = carrito.agregarProducto(1L, 5, 1, List.of());
        String id = original.get(0).getLineaId();
        original = carrito.agregarProducto(2L, 5, 1, original);
        List<CarritoItem> restante = carrito.modificarLinea(id, "quitar", original);
        assertThrows(IllegalArgumentException.class, () -> carrito.modificarLinea(id, "quitar", restante));
        assertThrows(IllegalArgumentException.class, () -> carrito.modificarLinea(id, "aumentar", restante));
        assertEquals(2L, restante.get(0).getProductoId());
    }

    @Test
    void agregarDesdeCatalogoConservaLaIdentidadDeLaLineaEditable() {
        List<CarritoItem> seleccion = carrito.agregarProducto(1L, 5, 1, List.of());
        String id = seleccion.get(0).getLineaId();
        seleccion = carrito.agregarProducto(1L, 5, 2, seleccion);
        assertEquals(1, seleccion.size());
        assertEquals(3, seleccion.get(0).getCantidad());
        assertEquals(id, seleccion.get(0).getLineaId());
    }

    @Test
    void promocionesSeRepitenYQuitanCompletasYElPedidoConservaSuDescuento() {
        List<CarritoItem> original = carrito.agregarPromocion(9L, List.of());
        String id = original.get(0).getLineaId();
        List<CarritoItem> doble = carrito.modificarLinea(id, "aumentar", original);
        assertEquals(6, doble.size());
        assertEquals(2, doble.stream().map(CarritoItem::getGrupoPack).distinct().count());
        assertEquals(6, doble.stream().map(CarritoItem::getLineaId).distinct().count());
        List<CarritoItem> restante = carrito.modificarLinea(id, "quitar", doble);
        assertEquals(3, restante.size());
        assertEquals(List.of(2, 1, 1), restante.stream().map(CarritoItem::getCantidad).toList());
        assertTrue(restante.stream().allMatch(i -> i.getPrecio().equals(new BigDecimal("35.10"))));
        assertEquals(new BigDecimal("140.40"), registrar(restante).getTotalPedido());
    }

    @Test
    void noSePuedeRomperLaComposicionDeUnaPromocion() {
        List<CarritoItem> original = carrito.agregarPromocion(9L, List.of());
        assertThrows(IllegalArgumentException.class, () -> carrito.modificarLinea(original.get(0).getLineaId(), "disminuir", original));
        assertEquals(2, original.get(0).getCantidad());
        assertEquals(3, original.size());
    }

    @Test
    void packsPersonalizadosConservanSusTresPerfumesYSeRegistran() {
        List<CarritoItem> original = carrito.agregarPackPersonalizado(List.of(
                new SeleccionPack(1L, 3, 1), new SeleccionPack(2L, 5, 2), new SeleccionPack(3L, 10, 1)), List.of());
        List<CarritoItem> doble = carrito.modificarLinea(original.get(1).getLineaId(), "aumentar", original);
        assertEquals(6, doble.size());
        assertEquals(2, doble.stream().map(CarritoItem::getGrupoPack).distinct().count());
        List<CarritoItem> restante = carrito.modificarLinea(original.get(2).getLineaId(), "quitar", doble);
        assertEquals(3, restante.size());
        assertTrue(restante.stream().allMatch(CarritoItem::isPackPersonalizado));
        assertEquals(new BigDecimal("140.40"), registrar(restante).getTotalPedido());
    }

    @Test
    void controladorGuardaSoloCambiosValidosEnLaSesionQueUsaraElPedido() {
        var controlador = new CarritoController(carrito);
        var sesion = new MockHttpSession();
        List<CarritoItem> original = carrito.agregarProducto(1L, 5, 1, List.of());
        sesion.setAttribute("carrito", original);
        var error = new RedirectAttributesModelMap();
        assertEquals("redirect:/carrito", controlador.modificar("otra-sesion", "quitar", sesion, error));
        assertTrue(error.getFlashAttributes().containsKey("error"));
        assertSame(original, sesion.getAttribute("carrito"));
        controlador.modificar(original.get(0).getLineaId(), "aumentar", sesion, new RedirectAttributesModelMap());
        @SuppressWarnings("unchecked")
        List<CarritoItem> actual = (List<CarritoItem>) sesion.getAttribute("carrito");
        assertEquals(2, actual.get(0).getCantidad());
        assertEquals(1, original.get(0).getCantidad());
        assertEquals(new BigDecimal("78.00"), registrar(actual).getTotalPedido());
    }

    private Pedido registrar(List<CarritoItem> seleccion) {
        PedidoRepository pedidos = mock(PedidoRepository.class);
        ProductoRepository repositorioProductos = mock(ProductoRepository.class);
        PresentacionRepository presentaciones = mock(PresentacionRepository.class);
        PackRepository packs = mock(PackRepository.class);
        when(packs.bloquearPorId(9L)).thenReturn(Optional.of(promocion));
        when(repositorioProductos.bloquearPorId(anyLong())).thenAnswer(i -> Optional.ofNullable(productos.get(i.getArgument(0))));
        when(presentaciones.bloquearPorProductoYml(anyLong(), anyInt())).thenAnswer(i -> Optional.ofNullable(productos.get(i.getArgument(0)).buscarPresentacion(i.getArgument(1))));
        Pedido[] guardado = new Pedido[1];
        when(pedidos.save(any())).thenAnswer(i -> {
            Pedido pedido = i.getArgument(0); pedido.setId(77L); guardado[0] = pedido; return pedido;
        });
        var servicio = new PedidoServiceImpl(pedidos, repositorioProductos, presentaciones, packs, catalogo, new PedidoValidador());
        assertEquals(77L, servicio.registrar("Prueba del carrito", "900000000", "Dirección de prueba 123", true, seleccion));
        return guardado[0];
    }
}
