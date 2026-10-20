package com.cibertec.app.repository;

import com.cibertec.app.entity.*;

import org.springframework.data.jpa.repository.*;

import java.util.*;

public interface ProductoImagenRepository extends JpaRepository<ProductoImagen, ProductoImagenId> {
    List<ProductoImagen> findByProductoIdOrderByOrdenAsc(Long id);

    void deleteByProductoId(Long id);
}
