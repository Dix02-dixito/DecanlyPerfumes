package com.cibertec.app.repository;
import com.cibertec.app.entity.Pack;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PackRepository extends JpaRepository<Pack,Long> {
    List<Pack> findByActivoTrueOrderByIdPackAsc();
}
