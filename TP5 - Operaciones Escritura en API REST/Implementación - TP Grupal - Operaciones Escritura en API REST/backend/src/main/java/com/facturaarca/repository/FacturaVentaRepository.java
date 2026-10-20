package com.facturaarca.repository;

import com.facturaarca.entities.FacturaVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface FacturaVentaRepository extends JpaRepository<FacturaVenta, Long> {

    @Query("SELECT COALESCE(MAX(f.numero), 0) FROM FacturaVenta f")
    Long obtenerMaxNumero();

    @Query("SELECT DISTINCT f FROM FacturaVenta f " +
           "LEFT JOIN FETCH f.cliente " +
           "JOIN FETCH f.condicionIva " +
           "JOIN FETCH f.puntoVenta " +
           "LEFT JOIN FETCH f.detalles " +
           "WHERE f.fechaBaja IS NULL " +
           "ORDER BY f.numero")
    List<FacturaVenta> findAllConRelaciones();

    @Query("SELECT DISTINCT f FROM FacturaVenta f " +
           "LEFT JOIN FETCH f.cliente " +
           "JOIN FETCH f.condicionIva " +
           "JOIN FETCH f.puntoVenta " +
           "LEFT JOIN FETCH f.detalles " +
           "WHERE f.fechaBaja IS NULL " +
           "AND (:fechaDesde IS NULL OR f.fechaEmision >= :fechaDesde) " +
           "AND (:fechaHasta IS NULL OR f.fechaEmision <= :fechaHasta) " +
           "AND (:estado IS NULL OR f.estado = :estado) " +
           "AND (:montoMinimo IS NULL OR f.importeTotal >= :montoMinimo) " +
           "ORDER BY f.numero")
    List<FacturaVenta> buscarFiltradas(
            @Param("fechaDesde") Date fechaDesde,
            @Param("fechaHasta") Date fechaHasta,
            @Param("estado") String estado,
            @Param("montoMinimo") Double montoMinimo);
}
