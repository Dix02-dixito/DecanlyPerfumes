package com.cibertec.app.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cibertec.app.config.SeguridadConfig;
import com.cibertec.app.dto.PedidoFormulario;
import com.cibertec.app.entity.*;
import com.cibertec.app.repository.*;
import com.cibertec.app.security.IntentosLogin;
import com.cibertec.app.service.*;
import com.cibertec.app.service.impl.UsuarioServiceImpl;

@WebMvcTest(PedidoController.class)
@Import({SeguridadConfig.class, UsuarioServiceImpl.class, IntentosLogin.class, WhatsAppService.class})
class PedidoWebTest {
    @Autowired MockMvc mvc;
    @MockitoBean PedidoService pedidos;
    @MockitoBean PedidoRepository repositorio;
    @MockitoBean CatalogoService catalogo;
    @MockitoBean UsuarioRepository usuarios;

    @BeforeEach void preparar() { when(catalogo.listar(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean())).thenReturn(List.of()); }

    @Test
    void soloAdminAccedeAlFormularioYAlRegistro() throws Exception {
        mvc.perform(get("/pedidos/nuevo")).andExpect(status().isUnauthorized());
        mvc.perform(post("/pedidos").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(get("/pedidos/nuevo").with(user("vendedor").roles("VENDEDOR"))).andExpect(status().isForbidden());
        mvc.perform(post("/pedidos").with(user("vendedor").roles("VENDEDOR")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/pedidos/nuevo").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(view().name("pedido/nuevo"));
        verifyNoInteractions(pedidos);
    }

    @Test
    void requiereCsrfYUnFormularioEmitidoPorElServidor() throws Exception {
        mvc.perform(post("/pedidos").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post("/pedidos").with(user("admin").roles("ADMIN")).with(csrf())
                .param("token", UUID.randomUUID().toString())).andExpect(status().isBadRequest());
        verifyNoInteractions(pedidos);
    }

    @Test
    void unReenvioDelFormularioRedirigeAlMismoPedido() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        var respuesta = mvc.perform(get("/pedidos/nuevo").session(sesion).with(user("admin").roles("ADMIN"))).andReturn();
        PedidoFormulario formulario = (PedidoFormulario) respuesta.getModelAndView().getModel().get("pedidoForm");
        when(pedidos.registrarManual(any())).thenReturn(42L);
        mvc.perform(post("/pedidos").session(sesion).with(user("admin").roles("ADMIN")).with(csrf())
                .param("token", formulario.getToken()).param("nombreCliente", "Cliente prueba")
                .param("id", "999").param("totalPedido", "0").param("estadoPedido", "PENDIENTE"))
                .andExpect(redirectedUrl("/pedidos/42"));
        Pedido existente = new Pedido(); existente.setId(42L);
        when(repositorio.findByTokenRegistro(formulario.getToken())).thenReturn(Optional.of(existente));
        mvc.perform(post("/pedidos").session(sesion).with(user("admin").roles("ADMIN")).with(csrf())
                .param("token", formulario.getToken())).andExpect(redirectedUrl("/pedidos/42"));
        verify(pedidos, times(1)).registrarManual(any());
    }

    @Test
    void carritoSolicitaPorWhatsAppSinGuardarPedidoNiVaciarSeleccion() throws Exception {
        Pedido solicitud = new Pedido(); solicitud.setNombreCliente("Cliente prueba");
        solicitud.setTelefono("900000000"); solicitud.setDireccion("Av. Prueba 123");
        solicitud.setTotalPedido(new BigDecimal("78.00"));
        Producto producto = new Producto(); producto.setNombre("Perfume de prueba");
        DetallePedido d = new DetallePedido(); d.setProducto(producto); d.setMl(5); d.setCantidad(2);
        solicitud.agregarDetalle(d);
        when(pedidos.prepararSolicitud(anyString(), anyString(), anyString(), anyBoolean(), anyList())).thenReturn(solicitud);
        MockHttpSession sesion = new MockHttpSession(); var carrito = new ArrayList<CarritoItem>(); carrito.add(new CarritoItem());
        sesion.setAttribute("carrito", carrito);
        var r = mvc.perform(post("/pedido/registrar").session(sesion).with(csrf())
                .param("nombreCliente", "Cliente prueba").param("telefono", "900000000")
                .param("direccion", "Av. Prueba 123").param("aceptaTerminos", "true"))
                .andExpect(status().isFound()).andReturn();
        String enlace = java.net.URLDecoder.decode(r.getResponse().getRedirectedUrl(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(enlace.startsWith("https://wa.me/51907032488?text="));
        assertTrue(enlace.contains("5 ml × 2")); assertTrue(enlace.contains("S/ 78.00"));
        assertFalse(enlace.contains("Pedido: #")); assertSame(carrito, sesion.getAttribute("carrito"));
        verify(pedidos, never()).registrarManual(any()); verifyNoInteractions(repositorio);
    }

    @Test
    void listadoYDetalleSonPrivadosYNoSeExponenLosAntiguosCambiosDeEstado() throws Exception {
        mvc.perform(get("/pedidos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/pedidos/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/pedidos/999").with(user("admin").roles("ADMIN"))).andExpect(status().isNotFound());
        mvc.perform(post("/pedido/1/estado").param("estado", "CONFIRMADO").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
