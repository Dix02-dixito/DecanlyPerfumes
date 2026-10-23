package com.cibertec.app.repository;

import com.cibertec.app.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository
        extends JpaRepository<Pedido, Long> {
}