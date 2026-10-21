package com.facturaarca.repository;

import com.facturaarca.entities.FacturaVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FacturaVentaRepository extends JpaRepository<FacturaVenta, Long> {

    @Query("SELECT COALESCE(MAX(f.numero), 0) FROM FacturaVenta f WHERE f.puntoVenta.id = :puntoVentaId")
    Long obtenerMaxNumeroPorPuntoVenta(@Param("puntoVentaId") Long puntoVentaId);
}
