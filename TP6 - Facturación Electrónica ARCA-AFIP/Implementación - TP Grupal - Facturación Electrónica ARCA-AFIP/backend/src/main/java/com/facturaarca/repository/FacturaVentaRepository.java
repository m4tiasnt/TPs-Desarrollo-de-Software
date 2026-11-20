package com.facturaarca.repository;

import com.facturaarca.entities.FacturaVenta;
import com.facturaarca.entities.Usuario;
import com.facturaarca.enums.EstadoFactura;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaVentaRepository extends JpaRepository<FacturaVenta, Long> {

    /**
     * Trae la factura con todas sus relaciones para armar el DTO de detalle
     * sin caer en LazyInitializationException.
     */
    @EntityGraph(attributePaths = {
            "cliente",
            "cliente.domicilio",
            "condicionIva",
            "puntoVenta",
            "tipoMoneda",
            "detalles",
            "detalles.listaPrecioArticulo",
            "detalles.listaPrecioArticulo.articulo",
            "detalles.listaPrecioArticulo.listaPrecio"
    })
    @Query("SELECT f FROM FacturaVenta f WHERE f.id = :id AND f.fechaBaja IS NULL")
    Optional<FacturaVenta> buscarConDetalles(@Param("id") Long id);

    /**
     * Cambia el estado de la factura a EN_PROCESO de forma atómica solo si está en un estado informable.
     * Devuelve 1 si la reservó esta petición, o 0 si no existe o ya estaba en otro estado.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE FacturaVenta f SET f.estado = :enProceso, f.fechaModificacion = :fecha, f.usuarioModificacion = :usuario " +
           "WHERE f.id = :id AND f.fechaBaja IS NULL AND f.estado IN :estadosInformables")
    int marcarEnProceso(
            @Param("id") Long id,
            @Param("enProceso") EstadoFactura enProceso,
            @Param("estadosInformables") List<EstadoFactura> estadosInformables,
            @Param("fecha") Date fecha,
            @Param("usuario") Usuario usuario);
}
