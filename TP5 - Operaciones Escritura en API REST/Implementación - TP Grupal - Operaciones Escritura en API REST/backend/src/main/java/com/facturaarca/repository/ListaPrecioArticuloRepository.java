package com.facturaarca.repository;

import com.facturaarca.dto.ListaPrecioArticuloDTO;
import com.facturaarca.entities.ListaPrecioArticulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListaPrecioArticuloRepository extends JpaRepository<ListaPrecioArticulo, Long> {

    Optional<ListaPrecioArticulo> findFirstByArticuloIdOrderByIdAsc(Long articuloId);

    @Query("SELECT new com.facturaarca.dto.ListaPrecioArticuloDTO(" +
           "   lpa.id, lp.id, lp.denominacion, lpa.precioVenta) " +
           "FROM ListaPrecioArticulo lpa " +
           "JOIN lpa.listaPrecio lp " +
           "WHERE lpa.articulo.id = :articuloId " +
           "ORDER BY lpa.id")
    List<ListaPrecioArticuloDTO> buscarListasPrecioPorArticulo(@Param("articuloId") Long articuloId);
}
