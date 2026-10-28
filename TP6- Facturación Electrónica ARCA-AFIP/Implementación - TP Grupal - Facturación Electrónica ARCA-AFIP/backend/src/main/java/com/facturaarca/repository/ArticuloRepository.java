package com.facturaarca.repository;

import com.facturaarca.dto.ArticuloSearchDTO;
import com.facturaarca.entities.Articulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticuloRepository extends JpaRepository<Articulo, Long> {

    @Query("SELECT new com.facturaarca.dto.ArticuloSearchDTO(" +
            "a.id, a.codigo, a.denominacion, m.denominacion, " +
            "COALESCE(lpa.precioVenta, 0), lpa.id) " +
            "FROM Articulo a " +
            "LEFT JOIN a.marca m " +
            "LEFT JOIN ListaPrecioArticulo lpa ON lpa.articulo.id = a.id " +
            "WHERE (LOWER(a.codigo) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "   OR LOWER(a.denominacion) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "   OR LOWER(m.denominacion) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "ORDER BY a.denominacion")
    List<ArticuloSearchDTO> buscarPorTermino(@Param("q") String q);
}
