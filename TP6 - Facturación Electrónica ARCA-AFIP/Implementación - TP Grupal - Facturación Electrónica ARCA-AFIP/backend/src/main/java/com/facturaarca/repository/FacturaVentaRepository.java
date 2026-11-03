package com.facturaarca.repository;

import com.facturaarca.entities.FacturaVenta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacturaVentaRepository extends JpaRepository<FacturaVenta, Long> {

    @Query("SELECT COALESCE(MAX(f.numero), 0) FROM FacturaVenta f WHERE f.puntoVenta.id = :puntoVentaId")
    Long obtenerMaxNumeroPorPuntoVenta(@Param("puntoVentaId") Long puntoVentaId);

    /**
     * Trae la factura con todas sus relaciones para armar el DTO de detalle
     * sin caer en LazyInitializationException.
     */
    @EntityGraph(attributePaths = {
            "cliente",
            "condicionIva",
            "puntoVenta",
            "tipoMoneda",
            "detalles",
            "detalles.listaPrecioArticulo",
            "detalles.listaPrecioArticulo.articulo",
            "detalles.listaPrecioArticulo.listaPrecio"
    })
    @Query("SELECT f FROM FacturaVenta f WHERE f.id = :id")
    Optional<FacturaVenta> buscarConDetalles(@Param("id") Long id);
}
