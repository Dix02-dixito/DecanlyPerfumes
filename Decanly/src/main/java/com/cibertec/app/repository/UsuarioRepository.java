package com.cibertec.app.repository;

import com.cibertec.app.entity.Usuario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
	Usuario findByCorreo(String correo);
}
