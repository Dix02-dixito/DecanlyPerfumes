package com.cibertec.app.repository;

import com.cibertec.app.entity.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    @EntityGraph(attributePaths = {"marca", "presentaciones"})
    List<Producto> findAllByOrderByIdDesc();

    @Override
    @EntityGraph(attributePaths = {"marca", "presentaciones"})
    Optional<Producto> findById(Long id);

    @Query(
            value =
                    "SELECT COUNT(*) FROM producto p WHERE p.activo=TRUE AND (p.imagen_id=:imagen"
                        + " OR EXISTS (SELECT 1 FROM producto_imagen pi WHERE pi.producto_id=p.id"
                        + " AND pi.imagen_id=:imagen))",
            nativeQuery = true)
    long contarImagenPublica(@Param("imagen") Long imagen);
}
