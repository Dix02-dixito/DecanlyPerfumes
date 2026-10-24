package com.cibertec.app.repository;

import com.cibertec.app.entity.Pedido;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

public interface PedidoRepository
        extends JpaRepository<Pedido, Long> {
	
	   @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select p from Pedido p where p.id = :id")
        Optional<Pedido> bloquearPorId(@Param("id") Long id);
	   
	   @EntityGraph(attributePaths = {"detalles", "detalles.producto"})
	   @Query("select distinct p from Pedido p where p.id = :id")
	   Optional<Pedido> buscarConDetalles(@Param("id") Long id);
	   
	   
	   @Query("""
		        select p from Pedido p
		        where (:id is null or p.id = :id)
		          and (:q = '' or lower(p.nombreCliente)
		               like lower(concat('%', :q, '%')))
		          and (:estado = '' or p.estadoPedido = :estado)
		        order by p.id desc
		        """)
		List<Pedido> buscar(
		        @Param("id") Long id,
		        @Param("q") String q,
		        @Param("estado") String estado);
}