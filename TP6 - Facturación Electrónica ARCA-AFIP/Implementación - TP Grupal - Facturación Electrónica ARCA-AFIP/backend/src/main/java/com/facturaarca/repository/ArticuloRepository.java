package com.facturaarca.repository;

import com.facturaarca.dto.ArticuloPrecioDTO;
import com.facturaarca.entities.Articulo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticuloRepository extends JpaRepository<Articulo, Long> {

    /**
     * Busca artículos por código, denominación o marca, pero solo devuelve
     * aquellos que tengan precio en la lista indicada.
     */
    @Query("SELECT DISTINCT new com.facturaarca.dto.ArticuloPrecioDTO(" +
            "a.id, a.codigo, a.denominacion, m.denominacion, " +
            "lpa.precioVenta, lpa.id, lp.id, lp.denominacion) " +
            "FROM Articulo a " +
            "LEFT JOIN a.marca m " +
            "JOIN ListaPrecioArticulo lpa ON lpa.articulo.id = a.id " +
            "JOIN lpa.listaPrecio lp " +
            "WHERE lp.id = :listaPrecioId " +
            "AND (LOWER(a.codigo) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "   OR LOWER(a.denominacion) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "   OR LOWER(m.denominacion) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "ORDER BY a.denominacion")
    List<ArticuloPrecioDTO> buscarPorTermino(@Param("q") String q,
                                               @Param("listaPrecioId") Long listaPrecioId);

    /**
     * Búsqueda paginada (LIMIT/OFFSET) de artículos para el Explorer modal.
     * Filtra por lista de precios y opcionalmente por código y/o denominación/marca.
     */
    @Query(
        value = "SELECT new com.facturaarca.dto.ArticuloPrecioDTO(" +
                "a.id, a.codigo, a.denominacion, COALESCE(m.denominacion, ''), " +
                "lpa.precioVenta, lpa.id, lp.id, lp.denominacion) " +
                "FROM Articulo a " +
                "LEFT JOIN a.marca m " +
                "JOIN ListaPrecioArticulo lpa ON lpa.articulo.id = a.id " +
                "JOIN lpa.listaPrecio lp " +
                "WHERE a.fechaBaja IS NULL AND lpa.fechaBaja IS NULL " +
                "AND lp.id = :listaPrecioId " +
                "AND (:codigo = '' OR LOWER(a.codigo) LIKE LOWER(CONCAT('%', :codigo, '%'))) " +
                "AND (:termino = '' OR LOWER(a.denominacion) LIKE LOWER(CONCAT('%', :termino, '%')) " +
                "     OR LOWER(COALESCE(m.denominacion, '')) LIKE LOWER(CONCAT('%', :termino, '%')) " +
                "     OR LOWER(a.codigo) LIKE LOWER(CONCAT('%', :termino, '%'))) " +
                "ORDER BY a.denominacion ASC, a.id ASC",
        countQuery = "SELECT COUNT(lpa.id) " +
                "FROM Articulo a " +
                "LEFT JOIN a.marca m " +
                "JOIN ListaPrecioArticulo lpa ON lpa.articulo.id = a.id " +
                "JOIN lpa.listaPrecio lp " +
                "WHERE a.fechaBaja IS NULL AND lpa.fechaBaja IS NULL " +
                "AND lp.id = :listaPrecioId " +
                "AND (:codigo = '' OR LOWER(a.codigo) LIKE LOWER(CONCAT('%', :codigo, '%'))) " +
                "AND (:termino = '' OR LOWER(a.denominacion) LIKE LOWER(CONCAT('%', :termino, '%')) " +
                "     OR LOWER(COALESCE(m.denominacion, '')) LIKE LOWER(CONCAT('%', :termino, '%')) " +
                "     OR LOWER(a.codigo) LIKE LOWER(CONCAT('%', :termino, '%')))"
    )
    Page<ArticuloPrecioDTO> buscarPaginadoPorListaPrecio(
            @Param("codigo") String codigo,
            @Param("termino") String termino,
            @Param("listaPrecioId") Long listaPrecioId,
            Pageable pageable);
}
