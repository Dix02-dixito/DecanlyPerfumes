package com.cibertec.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cibertec.app.entity.Presentacion;

import jakarta.persistence.LockModeType;

public interface PresentacionRepository
        extends JpaRepository<Presentacion, Long> {

    List<Presentacion> findByProducto_IdOrderByMlAsc(
            Long productoId);

    Optional<Presentacion> findByProducto_IdAndMl(
            Long productoId,
            Integer ml);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Presentacion p
            where p.producto.id = :productoId
              and p.ml = :ml
            """)
    Optional<Presentacion> bloquearPorProductoYml(
            @Param("productoId") Long productoId,
            @Param("ml") Integer ml);
}