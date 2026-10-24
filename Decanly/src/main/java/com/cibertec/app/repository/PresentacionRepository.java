package com.cibertec.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.app.entity.Presentacion;

public interface PresentacionRepository
        extends JpaRepository<Presentacion, Long> {

    List<Presentacion> findByProducto_IdOrderByMlAsc(
            Long productoId);

    Optional<Presentacion> findByProducto_IdAndMl(
            Long productoId,
            Integer ml);
}