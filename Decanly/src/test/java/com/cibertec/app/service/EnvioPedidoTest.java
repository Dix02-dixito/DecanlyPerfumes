package com.cibertec.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.mock.web.MockHttpSession;

import com.cibertec.app.controller.TiendaController;
import com.cibertec.app.entity.*;
import com.cibertec.app.repository.*;
import com.cibertec.app.service.impl.PedidoServiceImpl;
import com.cibertec.app.validation.PedidoValidador;

class EnvioPedidoTest {
    @ParameterizedTest
    @CsvSource({"15.00", "148.99", "149.00", "149.01", "200.00"})
    void carritoYSolicitudNoAgreganTarifaNiCreanPedidos(String precio) {
        var producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Perfume de prueba");
        var presentacion = new Presentacion();
        presentacion.setProducto(producto);
        presentacion.setMl(5);
        presentacion.setPrecio(new BigDecimal(precio));
        presentacion.setStock(2);
        var item = new CarritoItem();
        item.setProductoId(1L);
        item.setMl(5);
        item.setCantidad(1);
        item.setPrecio(presentacion.getPrecioFinal());
        var carrito = List.of(item);

        var sesion = new MockHttpSession();
        sesion.setAttribute("carrito", carrito);
        var modelo = new ExtendedModelMap();
        assertEquals("carrito/index", new TiendaController().carrito(sesion, modelo));
        assertEquals(0, new BigDecimal(precio).compareTo((BigDecimal) modelo.get("total")));

        var pedidos = mock(PedidoRepository.class);
        var productos = mock(ProductoRepository.class);
        var presentaciones = mock(PresentacionRepository.class);
        var packs = mock(PackRepository.class);
        var catalogo = mock(CatalogoService.class);
        when(productos.findById(1L)).thenReturn(Optional.of(producto));
        when(presentaciones.findByProducto_IdAndMl(1L, 5)).thenReturn(Optional.of(presentacion));
        var servicio = new PedidoServiceImpl(pedidos, productos, presentaciones, packs, catalogo, new PedidoValidador());
        Pedido solicitud = servicio.prepararSolicitud("Cliente de prueba", "999888777", "Av. Prueba 123", true, carrito);
        assertEquals(0, new BigDecimal(precio).compareTo(solicitud.getTotalPedido()));
        assertNull(solicitud.getId());
        verifyNoInteractions(pedidos);
        assertEquals(2, presentacion.getStock());
    }
}
