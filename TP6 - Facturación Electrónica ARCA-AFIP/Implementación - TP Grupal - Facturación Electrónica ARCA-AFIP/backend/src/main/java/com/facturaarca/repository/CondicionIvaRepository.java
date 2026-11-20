package com.facturaarca.repository;

import com.facturaarca.entities.CondicionIva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CondicionIvaRepository extends JpaRepository<CondicionIva, Long> {
    List<CondicionIva> findByFechaBajaIsNullOrderByIdAsc();
}
