package com.cibertec.app.repository;

import com.cibertec.app.entity.Producto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
	List<Producto> findAllByOrderByIdAsc();
}
