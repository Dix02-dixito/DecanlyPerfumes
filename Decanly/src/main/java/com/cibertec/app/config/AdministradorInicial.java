package com.cibertec.app.config;

import com.cibertec.app.entity.Usuario;
import com.cibertec.app.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Component
public class AdministradorInicial implements CommandLineRunner {
    @Autowired private UsuarioRepository repository;
    @Autowired private PasswordEncoder encoder;

    @Value("${decanly.admin.email}")
    private String email;

    @Value("${decanly.admin.password}")
    private String clave;

    @Override
    @Transactional
    public void run(String... args) {
        if (repository.count() > 0 || (email.isBlank() && clave.isBlank())) return;
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
                || email.length() > 120
                || clave.length() < 12
                || clave.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException(
                    "Configura ADMIN_EMAIL válido y ADMIN_PASSWORD de 12 caracteres como mínimo y"
                        + " 72 bytes máximo.");
        repository.save(
                new Usuario(
                        "Administrador",
                        email.trim().toLowerCase(java.util.Locale.ROOT),
                        encoder.encode(clave),
                        "ADMIN",
                        true));
    }
}
