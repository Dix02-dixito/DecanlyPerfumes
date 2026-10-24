package com.cibertec.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.app.entity.Pack;

public interface PackRepository extends JpaRepository<Pack, Long> {

    List<Pack> findAllByOrderByIdPackAsc();

    List<Pack> findByActivoTrueOrderByIdPackAsc();
}