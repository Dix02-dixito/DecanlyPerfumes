package com.cibertec.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.app.entity.Pack;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



public interface PackRepository extends JpaRepository<Pack, Long> {

    List<Pack> findAllByOrderByIdPackAsc();

    List<Pack> findByActivoTrueOrderByIdPackAsc();
    
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select p from Pack p where p.idPack = :id")
    Optional<Pack> bloquearPorId(@Param("id") Long id);
}