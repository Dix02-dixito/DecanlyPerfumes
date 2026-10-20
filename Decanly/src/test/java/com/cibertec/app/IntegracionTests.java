package com.cibertec.app;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cibertec.app.entity.*;
import com.cibertec.app.repository.*;
import com.cibertec.app.service.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;

import javax.imageio.ImageIO;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IntegracionTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ProductoService productos;
    @Autowired MarcaRepository marcas;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProductoRepository repository;
    @Autowired ImagenRepository imagenes;
    @Autowired PasswordEncoder encoder;
    MockHttpSession session;
    String token;
    Marca marca;
    final String clave = "ClaveSoloPrueba2026";

    @BeforeEach
    void preparar() throws Exception {
        for (String t :
                List.of(
                        "pack_detalle",
                        "pack",
                        "producto_imagen",
                        "presentacion",
                        "producto",
                        "imagen",
                        "marca",
                        "configuracion",
                        "usuario")) jdbc.update("DELETE FROM " + t);
        marca = new Marca();
        marca.setNombre("Dior");
        marca = marcas.save(marca);
        usuarios.save(
                new Usuario("Admin", "test@decanly.pe", encoder.encode(clave), "ADMIN", true));
        session = new MockHttpSession();
        token = csrf();
        login();
        token = csrf();
    }

    String csrf() throws Exception {
        return ((CsrfToken)
                        mvc.perform(get("/cuenta").session(session))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getRequest()
                                .getAttribute("_csrf"))
                .getToken();
    }

    void login() throws Exception {
        mvc.perform(
                        post("/api/sesion")
                                .session(session)
                                .header("X-CSRF-TOKEN", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"test@decanly.pe\",\"password\":\""
                                                + clave
                                                + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    MockMultipartFile foto(String campo) throws Exception {
        var b = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB), "png", b);
        return new MockMultipartFile(campo, "foto.png", "image/png", b.toByteArray());
    }

    Producto form(String nombre) {
        Producto v = productos.nuevoProducto();
        v.setNombre(nombre);
        v.setMarca(marca);
        for (Presentacion x : v.getPresentaciones()) {
            x.setPrecio(new BigDecimal("25.50"));
            x.setStock(5);
        }
        return v;
    }

    Producto guardar(String nombre) throws Exception {
        Producto v = productos.guardarProducto(form(nombre), foto("foto"), List.of());
        return productos.buscarProductoById(v.getId());
    }

    @Test
    void loginSesionPermisosYCsrf() throws Exception {
        mvc.perform(get("/api/sesion").session(session))
                .andExpect(jsonPath("$.user.email").value("test@decanly.pe"))
                .andExpect(content().string(not(containsString("claveHash"))));
        mvc.perform(get("/producto"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cuenta"));
        mvc.perform(post("/producto").session(session)).andExpect(status().isForbidden());
        String anterior = token;
        login();
        token = csrf();
        mvc.perform(
                        post("/producto/agotado/1")
                                .session(session)
                                .param("_csrf", anterior)
                                .param("version", "0"))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/sesion")
                                .session(session)
                                .header("X-CSRF-TOKEN", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"test@decanly.pe\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/sesion").session(session)).andExpect(jsonPath("$.user").isEmpty());
    }

    @Test
    void cuentaInactivaYLectorNoAdministran() throws Exception {
        Usuario admin = usuarios.findByCorreoIgnoreCase("test@decanly.pe").orElseThrow();
        admin.setActivo(false);
        usuarios.save(admin);
        mvc.perform(
                        post("/api/sesion")
                                .session(session)
                                .header("X-CSRF-TOKEN", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"test@decanly.pe\",\"password\":\""
                                                + clave
                                                + "\"}"))
                .andExpect(status().isUnauthorized());
        admin.setActivo(true);
        admin.setRol("LECTOR");
        usuarios.save(admin);
        token = csrf();
        loginLector();
        token = csrf();
        mvc.perform(get("/producto").session(session)).andExpect(status().isForbidden());
    }

    void loginLector() throws Exception {
        mvc.perform(
                        post("/api/sesion")
                                .session(session)
                                .header("X-CSRF-TOKEN", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"test@decanly.pe\",\"password\":\""
                                                + clave
                                                + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("LECTOR"));
    }

    @Test
    void crearEditarVersionRetirarRestaurarYFotos() throws Exception {
        Producto v = guardar("Mi perfume");
        assertEquals(3, v.getPresentaciones().size());
        Long id = v.getId(), imagen = v.getImagen().getId();
        mvc.perform(get("/imagenes/" + imagen))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        Producto cambio = form("Perfume editado");
        cambio.setVersion(v.getVersion());
        cambio.getPresentaciones().get(1).setStock(9);
        productos.actualizarProducto(id, cambio, null, List.of(foto("fotosGaleria")), List.of());
        v = productos.buscarProductoById(id);
        assertEquals(9, v.getPresentaciones().get(1).getStock());
        assertEquals(1, productos.listarGaleria(id).size());
        assertEquals(imagen, v.getImagen().getId());
        assertThrows(
                IllegalArgumentException.class,
                () -> productos.actualizarProducto(id, cambio, null, List.of(), List.of()));
        productos.eliminarProductoById(id, v.getVersion());
        mvc.perform(get("/imagenes/" + imagen)).andExpect(status().isNotFound());
        mvc.perform(get("/imagenes/" + imagen).session(session)).andExpect(status().isOk());
        v = productos.buscarProductoById(id);
        productos.restaurarProductoById(id, v.getVersion());
        v = productos.buscarProductoById(id);
        productos.agotarProductoById(id, v.getVersion());
        assertFalse(productos.buscarProductoById(id).isConStock());
        mvc.perform(get("/producto/delete/" + id).session(session))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void erroresRevientenTodaLaTransaccion() throws Exception {
        long antes = imagenes.count();
        var mala =
                new MockMultipartFile(
                        "fotosGaleria", "falsa.png", "image/png", "no es imagen".getBytes());
        assertThrows(
                IllegalArgumentException.class,
                () -> productos.guardarProducto(form("Inválido"), foto("foto"), List.of(mala)));
        assertEquals(0, repository.count());
        assertEquals(antes, imagenes.count());
        Producto a = guardar("A"), b = guardar("B");
        Producto cambio = form("A editado");
        cambio.setVersion(a.getVersion());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        productos.actualizarProducto(
                                a.getId(),
                                cambio,
                                null,
                                List.of(),
                                List.of(b.getImagen().getId())));
        assertEquals("A", productos.buscarProductoById(a.getId()).getNombre());
    }

    @Test
    void postMvcYLogoutNativo() throws Exception {
        var req =
                multipart("/producto")
                        .file(foto("foto"))
                        .file(foto("fotosGaleria"))
                        .session(session)
                        .param("_csrf", token)
                        .param("nombre", "Desde formulario")
                        .param("marca.id", marca.getId().toString())
                        .param("ocasion", "Día")
                        .param("descuento", "0");
        for (int i = 0; i < 3; i++)
            req.param("presentaciones[" + i + "].precio", "25.50")
                    .param("presentaciones[" + i + "].stock", "4");
        mvc.perform(req)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/producto"));
        assertEquals(1, repository.count());
        mvc.perform(post("/logout").session(session).param("_csrf", token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cuenta"));
    }

    void pack(Producto p) {
        jdbc.update(
                "INSERT INTO pack(id,titulo,descripcion,etiqueta,activo,version) VALUES(91,'Pack"
                    + " real','Desde MySQL','Favorito',TRUE,0)");
        jdbc.update(
                "INSERT INTO pack_detalle(pack_id,producto_id,ml,cantidad) VALUES(91,?,10,2)",
                p.getId());
    }

    @Test
    void packsJsonCorrectoSinProductosRetirados() throws Exception {
        Producto p = guardar("Perfume");
        pack(p);
        mvc.perform(get("/api/packs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(91))
                .andExpect(jsonPath("$[0].items[0].id").value(p.getId()))
                .andExpect(jsonPath("$[0].items[0].size").value("10"))
                .andExpect(jsonPath("$[0].items[0].qty").value(2))
                .andExpect(jsonPath("$[0].items[0].producto").doesNotExist());
        productos.eliminarProductoById(p.getId(), p.getVersion());
        mvc.perform(get("/api/packs")).andExpect(content().json("[]"));
        mvc.perform(
                        post("/api/packs")
                                .session(session)
                                .header("X-CSRF-TOKEN", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void vistasRealesYDatosParaFrontend() throws Exception {
        Producto p = guardar("Perfume de integración");
        pack(p);
        Path out = Path.of("target/frontend-qa");
        Files.createDirectories(out);
        Map<String, String> rutas = new LinkedHashMap<>();
        rutas.put("inicio", "/");
        for (String n :
                List.of(
                        "catalogo",
                        "packs",
                        "promociones",
                        "carrito",
                        "contacto",
                        "cuenta",
                        "producto",
                        "promocion",
                        "terminos",
                        "envios",
                        "privacidad")) rutas.put(n, "/" + n);
        rutas.put("create", "/producto/new");
        rutas.put("edit", "/producto/edit/" + p.getId());
        rutas.put("pendiente", "/pedidos");
        for (var e : rutas.entrySet()) {
            var res =
                    mvc.perform(get(e.getValue()).session(session))
                            .andExpect(status().isOk())
                            .andExpect(content().string(not(containsString("api-local.js"))))
                            .andReturn();
            Files.writeString(
                    out.resolve(e.getKey() + ".html"),
                    res.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        }
        Files.writeString(
                out.resolve("packs.json"),
                mvc.perform(get("/api/packs")).andReturn().getResponse().getContentAsString());
        Files.writeString(
                out.resolve("login.html"),
                mvc.perform(get("/cuenta"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        Files.writeString(
                out.resolve("contexto.html"),
                mvc.perform(get("/decanly/catalogo").contextPath("/decanly"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        mvc.perform(delete("/api/sesion").session(session).header("X-CSRF-TOKEN", token))
                .andExpect(status().isNoContent());
    }
}
