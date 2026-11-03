package com.facturaarca.repository;

import com.facturaarca.entities.FacturaVentaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FacturaVentaDetalleRepository extends JpaRepository<FacturaVentaDetalle, Long> {
}
