package com.cibertec.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.app.entity.PackDetalle;
import com.cibertec.app.entity.PackDetalleId;

public interface PackDetalleRepository
        extends JpaRepository<PackDetalle, PackDetalleId> {

    List<PackDetalle> findByPackIdOrderByProductoIdAsc(Long packId);
}