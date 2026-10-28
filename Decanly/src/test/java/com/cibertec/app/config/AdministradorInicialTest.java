package com.cibertec.app.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.mockito.ArgumentCaptor;
import com.cibertec.app.entity.Usuario;
import com.cibertec.app.entity.RolUsuario;
import com.cibertec.app.repository.UsuarioRepository;

class AdministradorInicialTest {
    @Test void guardaHashYNoLaClaveOriginal() {
        var repo = mock(UsuarioRepository.class);
        var encoder = new BCryptPasswordEncoder(4);
        String clave = "ClaveSoloParaPruebas2026";
        new AdministradorInicial(repo, encoder, "ADMIN@example.test", clave).run(null);
        var captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repo).save(captor.capture());
        var admin = captor.getValue();
        assertEquals("admin@example.test", admin.getCorreo());
        assertEquals(RolUsuario.ADMIN, admin.getRol());
        assertNotEquals(clave, admin.getClaveHash());
        assertTrue(encoder.matches(clave, admin.getClaveHash()));
    }
    @Test void nuncaCambiaUsuariosExistentes() {
        var repo = mock(UsuarioRepository.class);
        when(repo.count()).thenReturn(1L);
        new AdministradorInicial(repo, new BCryptPasswordEncoder(4), "", "").run(null);
        verify(repo, never()).save(any());
    }
    @Test void rechazaCredencialesIncompletas() {
        var repo = mock(UsuarioRepository.class);
        assertThrows(IllegalStateException.class, () ->
            new AdministradorInicial(repo, new BCryptPasswordEncoder(4), "admin@example.test", "corta").run(null));
        verify(repo, never()).save(any());
    }
}
