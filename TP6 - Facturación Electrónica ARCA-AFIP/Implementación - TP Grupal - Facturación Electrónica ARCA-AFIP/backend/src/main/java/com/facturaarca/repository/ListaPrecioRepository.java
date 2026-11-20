package com.facturaarca.repository;

import com.facturaarca.entities.ListaPrecio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListaPrecioRepository extends JpaRepository<ListaPrecio, Long> {
    List<ListaPrecio> findByFechaBajaIsNullOrderByIdAsc();
}
