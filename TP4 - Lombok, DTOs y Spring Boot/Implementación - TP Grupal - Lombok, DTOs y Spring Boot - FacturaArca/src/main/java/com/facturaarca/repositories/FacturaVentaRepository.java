package com.facturaarca.repositories;

import com.facturaarca.dto.FacturaReporteDTO;
import com.facturaarca.entities.FacturaVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FacturaVentaRepository extends JpaRepository<FacturaVenta, Long> {

    @Query("""
        SELECT new com.facturaarca.dto.FacturaReporteDTO(
            f.numero,
            f.fechaEmision,
            COALESCE(c.denominacion, 'Consumidor Final'),
            ci.denominacion,
            pv.descripcion,
            f.importeTotal,
            COUNT(d)
        )
        FROM FacturaVenta f
        LEFT JOIN f.cliente c
        JOIN f.condicionIva ci
        JOIN f.puntoVenta pv
        JOIN f.detalles d
        GROUP BY f.id,
                 f.numero,
                 f.fechaEmision,
                 c.denominacion,
                 ci.denominacion,
                 pv.descripcion,
                 f.importeTotal
        """)
    List<FacturaReporteDTO> generarReporte();
}