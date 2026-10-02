package com.facturaarca.repositories;

import com.facturaarca.dto.FacturaReporteDTO;
import com.facturaarca.FacturaVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacturaVentaRepository extends JpaRepository<FacturaVenta, Long> {

    @Query("SELECT new com.facturaarca.entities.dto.FacturaReporteDTO(f.id, f.letra, f.numero, f.cliente.razonSocial, f.total) FROM FacturaVenta f")
    List<FacturaReporteDTO> obtenerReporteFacturas();
}