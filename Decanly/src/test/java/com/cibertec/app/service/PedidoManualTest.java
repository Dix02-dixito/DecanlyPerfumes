package com.cibertec.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.cibertec.app.dto.PedidoFormulario;
import com.cibertec.app.entity.*;
import com.cibertec.app.repository.*;
import com.cibertec.app.service.impl.PedidoServiceImpl;
import com.cibertec.app.validation.PedidoValidador;

class PedidoManualTest {
    final PedidoRepository pedidos = mock(PedidoRepository.class);
    final ProductoRepository productos = mock(ProductoRepository.class);
    final PresentacionRepository presentaciones = mock(PresentacionRepository.class);
    final PedidoServiceImpl servicio = new PedidoServiceImpl(pedidos, productos, presentaciones,
            mock(PackRepository.class), mock(CatalogoService.class), new PedidoValidador());
    final Map<Long, Producto> catalogo = new HashMap<>();
    Pedido guardado;

    @BeforeEach
    void preparar() {
        for (long id = 1; id <= 2; id++) {
            Producto producto = new Producto(); producto.setId(id); producto.setNombre("Perfume " + id);
            Presentacion v = new Presentacion(); v.setProducto(producto); v.setMl(5);
            v.setPrecio(new BigDecimal("50.00")); v.setStock(6); producto.getPresentaciones().add(v);
            catalogo.put(id, producto);
            when(productos.bloquearPorId(id)).thenReturn(Optional.of(producto));
            when(presentaciones.bloquearPorProductoYml(id, 5)).thenReturn(Optional.of(v));
        }
        when(pedidos.saveAndFlush(any())).thenAnswer(i -> {
            guardado = i.getArgument(0); guardado.setId(81L); return guardado;
        });
    }

    @Test
    void registraVentaCompletaConFechaRealPreciosHistoricosYDescuentoSinEnvio() {
        PedidoFormulario f = formulario();
        f.getLineas().add(linea(2L, 1, "40.00")); f.setDescuento(new BigDecimal("17.50"));
        assertEquals(81L, servicio.registrarManual(f));
        assertEquals(new BigDecimal("117.50"), guardado.getSubtotalPedido());
        assertEquals(new BigDecimal("100.00"), guardado.getTotalPedido());
        assertEquals(new BigDecimal("17.50"), guardado.getDescuentoPedido());
        assertEquals(f.getFechaPedido().atStartOfDay(), guardado.getFechaPedido());
        assertNotNull(guardado.getFechaRegistro()); assertTrue(guardado.isRegistroManual());
        assertEquals("ENTREGADO", guardado.getEstadoPedido()); assertEquals("", guardado.getDireccion());
        assertEquals("Cliente prueba", guardado.getNombreCliente()); assertEquals("+51907032488", guardado.getTelefono());
        assertEquals(4, catalogo.get(1L).buscarPresentacion(5).getStock());
        assertEquals(5, catalogo.get(2L).buscarPresentacion(5).getStock());
        assertEquals(2, guardado.getDetalles().get(0).getCantidad());
        assertEquals(new BigDecimal("38.75"), guardado.getDetalles().get(0).getPrecio());
        assertSame(guardado, guardado.getDetalles().get(0).getPedido());
        catalogo.get(1L).setNombre("Nombre modificado después");
        catalogo.get(1L).buscarPresentacion(5).setPrecio(new BigDecimal("90.00"));
        assertEquals("Perfume 1, Perfume 2", guardado.getPerfumesComprados());
        assertEquals(new BigDecimal("100.00"), guardado.getTotalPedido());
    }

    @Test
    void repetirTokenDevuelveMismoIdSinRestarExistencias() {
        Pedido existente = new Pedido(); existente.setId(10L);
        PedidoFormulario f = formulario(); when(pedidos.findByTokenRegistro(f.getToken())).thenReturn(Optional.of(existente));
        assertEquals(10L, servicio.registrarManual(f));
        verifyNoInteractions(productos, presentaciones); verify(pedidos, never()).saveAndFlush(any());
    }

    @Test
    void variasLineasDeLaMismaPresentacionCompartenStock() {
        PedidoFormulario f = formulario(); f.getLineas().add(linea(1L, 5, "38.75"));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarManual(f));
        assertEquals(6, catalogo.get(1L).buscarPresentacion(5).getStock());
        verify(pedidos, never()).saveAndFlush(any());
    }

    @Test
    void unSegundoProductoSinStockNoDescuentaElPrimero() {
        PedidoFormulario f = formulario(); f.getLineas().add(linea(2L, 7, "10.00"));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarManual(f));
        assertEquals(6, catalogo.get(1L).buscarPresentacion(5).getStock());
        assertEquals(6, catalogo.get(2L).buscarPresentacion(5).getStock());
    }

    @Test
    void perfumeInactivoOTamanoInexistenteNoSeGuarda() {
        catalogo.get(1L).setActivo(false);
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarManual(formulario()));
        catalogo.get(1L).setActivo(true);
        PedidoFormulario f = formulario(); f.getLineas().get(0).setMl(10);
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarManual(f));
        verify(pedidos, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "77.51", "100000000.00", "0.001"})
    void rechazaDescuentosInvalidosAntesDeTocarStock(String valor) {
        PedidoFormulario f = formulario(); f.setDescuento(new BigDecimal(valor));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarManual(f));
        verifyNoInteractions(productos, presentaciones); verify(pedidos, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-2", "0", "0.001", "100000000"})
    void rechazaPreciosInvalidos(String valor) {
        PedidoFormulario f = formulario(); f.getLineas().get(0).setPrecioUnitario(new BigDecimal(valor));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarManual(f));
        verifyNoInteractions(pedidos, productos, presentaciones);
    }

    @Test
    void limitaCantidadesImporteTotalYFechaFutura() {
        PedidoFormulario f = formulario(); f.getLineas().get(0).setCantidad(0);
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarManual(f));
        f.getLineas().get(0).setCantidad(10000);
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarManual(f));
        f.getLineas().get(0).setCantidad(2); f.getLineas().get(0).setPrecioUnitario(new BigDecimal("99999999.99"));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarManual(f));
        f.getLineas().get(0).setPrecioUnitario(new BigDecimal("1.00"));
        f.setFechaPedido(LocalDate.now(PedidoValidador.ZONA_VENTA).plusDays(1));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarManual(f));
        verifyNoInteractions(productos, presentaciones);
    }

    @Test
    void descuentoIgualASubtotalEsValidoYSeConservaCompleto() {
        PedidoFormulario f = formulario(); f.setDescuento(new BigDecimal("77.50"));
        servicio.registrarManual(f);
        assertEquals(new BigDecimal("0.00"), guardado.getTotalPedido());
        assertEquals(new BigDecimal("77.50"), guardado.getDescuentoPedido());
    }

    private PedidoFormulario formulario() {
        PedidoFormulario f = new PedidoFormulario(); f.setToken(UUID.randomUUID().toString());
        f.setNombreCliente(" Cliente prueba "); f.setTelefono(" +51907032488 ");
        f.setFechaPedido(LocalDate.now(PedidoValidador.ZONA_VENTA).minusDays(3));
        f.getLineas().add(linea(1L, 2, "38.75")); return f;
    }

    private PedidoFormulario.Linea linea(Long id, int cantidad, String precio) {
        var l = new PedidoFormulario.Linea(); l.setProductoId(id); l.setMl(5);
        l.setCantidad(cantidad); l.setPrecioUnitario(new BigDecimal(precio)); return l;
    }
}
