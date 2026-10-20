package com.cibertec.app.repository;

import com.cibertec.app.entity.*;

import org.springframework.data.jpa.repository.*;

import java.util.*;

public interface MarcaRepository extends JpaRepository<Marca, Long> {
    List<Marca> findAllByOrderByNombreAsc();
}
