package com.cibertec.app.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.cibertec.app.config.SeguridadConfig;
import com.cibertec.app.controller.SesionController;
import com.cibertec.app.entity.RolUsuario;
import com.cibertec.app.entity.Usuario;
import com.cibertec.app.repository.UsuarioRepository;
import com.cibertec.app.service.impl.UsuarioServiceImpl;

@WebMvcTest(SesionController.class)
@Import({SeguridadConfig.class, UsuarioServiceImpl.class, IntentosLogin.class})
class LoginProtegidoWebTest {
    private static final String CLAVE = "CorrectaSoloParaPruebas2026";
    private static final String HASH = new BCryptPasswordEncoder(4).encode(CLAVE);
    private static final AtomicInteger CONEXION = new AtomicInteger();
    @Autowired MockMvc mvc;
    @MockitoBean UsuarioRepository usuarios;
    private String ip;

    @BeforeEach
    void prepararUsuario() {
        ip = "192.0.2." + CONEXION.incrementAndGet();
        when(usuarios.findByCorreoIgnoreCase(anyString())).thenAnswer(invocacion -> {
            String correo = invocacion.getArgument(0);
            if (!correo.toLowerCase().startsWith("admin")) return Optional.empty();
            Usuario usuario = new Usuario("Administrador", correo.toLowerCase(), HASH, RolUsuario.ADMIN, true);
            usuario.setIdUsuario(1);
            return Optional.of(usuario);
        });
    }

    @Test
    void formularioYApiCompartenExactamenteCincoFallosAunqueCambieLaSesion() throws Exception {
        for (int i = 0; i < 3; i++) mvc.perform(formulario("admin@decanly.test", "incorrecta"))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/cuenta?error"));
        mvc.perform(api("admin@decanly.test", "incorrecta")).andExpect(status().isUnauthorized());
        mvc.perform(api(" ADMIN@DECANLY.TEST ", "incorrecta"))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
        mvc.perform(formulario("admin@decanly.test", CLAVE))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/cuenta?bloqueado"))
                .andExpect(header().exists("Retry-After"));
        var bloqueado = mvc.perform(api("admin@decanly.test", CLAVE))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("bloqueado temporalmente")))
                .andReturn();
        long segundos = Long.parseLong(bloqueado.getResponse().getHeader("Retry-After"));
        assertTrue(segundos > 0 && segundos <= 900);
        var sesion = bloqueado.getRequest().getSession(false);
        assertTrue(sesion == null || sesion.getAttribute("SPRING_SECURITY_CONTEXT") == null);
    }

    @Test
    void equivalenciasDelCorreoEnMySqlCompartenElBloqueo() throws Exception {
        Usuario usuario = new Usuario("Administrador", "admin@decanly.test", HASH, RolUsuario.ADMIN, true);
        usuario.setIdUsuario(1);
        when(usuarios.findByCorreoIgnoreCase("ádmin@decanly.test")).thenReturn(Optional.of(usuario));
        for (int i = 0; i < 4; i++) mvc.perform(api("admin@decanly.test", "incorrecta"))
                .andExpect(status().isUnauthorized());
        mvc.perform(api("ádmin@decanly.test", "incorrecta"))
                .andExpect(status().isTooManyRequests());
        mvc.perform(formulario("admin@decanly.test", CLAVE))
                .andExpect(redirectedUrl("/cuenta?bloqueado"));
    }

    @Test
    void correoInexistenteTieneElMismoBloqueoYRespuesta() throws Exception {
        for (int i = 0; i < 4; i++) mvc.perform(api("nadie@decanly.test", "incorrecta"))
                .andExpect(status().isUnauthorized());
        mvc.perform(api("nadie@decanly.test", "incorrecta"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("bloqueado temporalmente")));
    }

    @Test
    void otroCorreoYOtraIpPuedenAutenticarse() throws Exception {
        for (int i = 0; i < 5; i++) mvc.perform(api("admin@decanly.test", "incorrecta"));
        mvc.perform(api("admin2@decanly.test", CLAVE)).andExpect(status().isOk());
        mvc.perform(api("admin@decanly.test", CLAVE).with(r -> { r.setRemoteAddr("198.51.100.22"); return r; }))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("ADMIN"));
        mvc.perform(api("admin@decanly.test", CLAVE)).andExpect(status().isTooManyRequests());
    }

    @Test
    void accesoCorrectoReiniciaFallosYConservaSesionYLogout() throws Exception {
        for (int i = 0; i < 4; i++) mvc.perform(formulario("admin@decanly.test", "incorrecta"))
                .andExpect(redirectedUrl("/cuenta?error"));
        var acceso = mvc.perform(formulario("admin@decanly.test", CLAVE))
                .andExpect(redirectedUrl("/producto")).andReturn();
        MockHttpSession sesion = (MockHttpSession) acceso.getRequest().getSession(false);
        mvc.perform(get("/api/sesion").session(sesion)).andExpect(jsonPath("$.user.role").value("ADMIN"));
        mvc.perform(post("/cuenta/logout").session(sesion).with(csrf())).andExpect(redirectedUrl("/cuenta?logout"));
        for (int i = 0; i < 4; i++) mvc.perform(api("admin@decanly.test", "incorrecta"))
                .andExpect(status().isUnauthorized());
        mvc.perform(api("admin@decanly.test", CLAVE)).andExpect(status().isOk());
    }

    @Test
    void solicitudesSinCsrfNoAutenticanNiConsumenIntentos() throws Exception {
        for (int i = 0; i < 4; i++) mvc.perform(api("admin@decanly.test", "incorrecta"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/cuenta/login").param("correo", "admin@decanly.test").param("clave", CLAVE)
                .with(r -> { r.setRemoteAddr(ip); return r; })).andExpect(status().isForbidden());
        mvc.perform(post("/api/sesion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@decanly.test\",\"password\":\"" + CLAVE + "\"}")
                .with(r -> { r.setRemoteAddr(ip); return r; })).andExpect(status().isForbidden());
        mvc.perform(formulario("admin@decanly.test", CLAVE)).andExpect(redirectedUrl("/producto"));
    }

    private MockHttpServletRequestBuilder formulario(String correo, String clave) {
        return post("/cuenta/login").param("correo", correo).param("clave", clave)
                .with(csrf()).with(r -> { r.setRemoteAddr(ip); return r; });
    }

    private MockHttpServletRequestBuilder api(String correo, String clave) {
        return post("/api/sesion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + correo + "\",\"password\":\"" + clave + "\"}")
                .with(csrf()).with(r -> { r.setRemoteAddr(ip); return r; });
    }
}
