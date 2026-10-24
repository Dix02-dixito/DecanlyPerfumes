package com.cibertec.app.repository;

import com.cibertec.app.entity.Producto;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
	List<Producto> findAllByOrderByIdAsc();
	boolean existsByMarca_Id(Long marcaId);
	@Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
	@Query("select p from Producto p where p.id = :id")
	Optional<Producto> bloquearPorId(@Param("id") Long id);
	
}
