package com.facturaarca.repository;

import com.facturaarca.entities.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @Query("SELECT c FROM Cliente c WHERE " +
           "LOWER(c.denominacion) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "c.cuitCuil LIKE CONCAT('%', :filtro, '%')")
    Page<Cliente> findByDenominacionOrCuitCuil(@Param("filtro") String filtro, Pageable pageable);

    @Query("SELECT c FROM Cliente c WHERE c.fechaBaja IS NULL AND " +
           "(:razonSocial = '' OR LOWER(c.denominacion) LIKE LOWER(CONCAT('%', :razonSocial, '%'))) AND " +
           "(:cuit = '' OR c.cuitCuil LIKE CONCAT('%', :cuit, '%') OR " +
           " (:cuitSinGuiones <> '' AND REPLACE(c.cuitCuil, '-', '') LIKE CONCAT('%', :cuitSinGuiones, '%')))")
    Page<Cliente> buscarPorCuitYRazonSocial(
            @Param("cuit") String cuit,
            @Param("cuitSinGuiones") String cuitSinGuiones,
            @Param("razonSocial") String razonSocial,
            Pageable pageable);
}
