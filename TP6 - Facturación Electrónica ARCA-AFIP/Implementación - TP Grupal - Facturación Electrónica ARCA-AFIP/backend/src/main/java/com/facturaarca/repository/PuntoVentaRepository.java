package com.facturaarca.repository;

import com.facturaarca.entities.PuntoVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PuntoVentaRepository extends JpaRepository<PuntoVenta, Long> {
    List<PuntoVenta> findByFechaBajaIsNullOrderByNumeroAsc();
}
