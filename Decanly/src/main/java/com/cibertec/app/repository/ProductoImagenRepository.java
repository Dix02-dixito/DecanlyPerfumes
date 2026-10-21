package com.cibertec.app.repository;

import com.cibertec.app.entity.ProductoImagen;
import com.cibertec.app.entity.ProductoImagenId;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoImagenRepository extends JpaRepository<ProductoImagen, ProductoImagenId> {
	List<ProductoImagen> findByProductoIdOrderByOrdenAsc(Long productoId);
}
