package com.cibertec.app.repository;

import com.cibertec.app.entity.Marca;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarcaRepository extends JpaRepository<Marca, Long> {
	List<Marca> findAllByOrderByNombreAsc();
	Optional<Marca> findByNombreIgnoreCase(String nombre);
}
