package com.cibertec.app.repository;

import com.cibertec.app.entity.*;

import org.springframework.data.jpa.repository.*;

import java.util.*;

public interface PackRepository extends JpaRepository<Pack, Long> {
    @EntityGraph(attributePaths = {"detalles", "detalles.producto"})
    List<Pack> findByActivoTrueOrderByIdPackAsc();
}
