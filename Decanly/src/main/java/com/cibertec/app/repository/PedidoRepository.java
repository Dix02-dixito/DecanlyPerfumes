package com.cibertec.app.repository;

import com.cibertec.app.entity.Pedido;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.EntityGraph;

public interface PedidoRepository
        extends JpaRepository<Pedido, Long> {
	
	   @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select p from Pedido p where p.id = :id")
        Optional<Pedido> bloquearPorId(@Param("id") Long id);
}