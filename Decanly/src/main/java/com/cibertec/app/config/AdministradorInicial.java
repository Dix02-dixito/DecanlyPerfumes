package com.cibertec.app.config;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.app.entity.RolUsuario;
import com.cibertec.app.entity.Usuario;
import com.cibertec.app.repository.UsuarioRepository;

/** Alta inicial explícita; nunca reemplaza ni restablece usuarios existentes. */
@Component
@ConditionalOnProperty(name = "decanly.bootstrap-admin.enabled", havingValue = "true")
public class AdministradorInicial implements ApplicationRunner {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String correo;
    private final String clave;

    public AdministradorInicial(UsuarioRepository usuarios, PasswordEncoder encoder,
            @Value("${ADMIN_EMAIL:}") String correo, @Value("${ADMIN_PASSWORD:}") String clave) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.correo = correo;
        this.clave = clave;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // El reinicio con la opción aún activa no cambia ninguna cuenta.
        if (usuarios.count() != 0) return;
        String email = correo.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 150 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalStateException("Configura ADMIN_EMAIL para crear el primer administrador.");
        }
        if (clave.length() < 16 || clave.getBytes(StandardCharsets.UTF_8).length > 72 || clave.isBlank()) {
            throw new IllegalStateException("ADMIN_PASSWORD debe tener al menos 16 caracteres y no superar 72 bytes.");
        }
        usuarios.save(new Usuario("Administrador", email, encoder.encode(clave), RolUsuario.ADMIN, true));
    }
}
