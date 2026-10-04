package com.facturaarca.repositories;

import com.facturaarca.dto.FacturaReporteDTO;
import com.facturaarca.entities.FacturaVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FacturaVentaRepository extends JpaRepository<FacturaVenta, Long> {



}
