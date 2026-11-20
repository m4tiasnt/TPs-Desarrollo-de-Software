package com.facturaarca.repository;

import com.facturaarca.dto.ClienteDTO;
import com.facturaarca.entities.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @Query("SELECT new com.facturaarca.dto.ClienteDTO(c.id, c.cuitCuil, c.denominacion) " +
           "FROM Cliente c WHERE c.fechaBaja IS NULL ORDER BY c.denominacion ASC")
    List<ClienteDTO> listarActivos();

    @Query(
        value = "SELECT new com.facturaarca.dto.ClienteDTO(c.id, c.cuitCuil, c.denominacion) " +
                "FROM Cliente c WHERE c.fechaBaja IS NULL AND " +
                "(:razonSocial = '' OR LOWER(c.denominacion) LIKE LOWER(CONCAT('%', :razonSocial, '%')) ESCAPE '!') AND " +
                "(:cuit = '' OR c.cuitCuil LIKE CONCAT('%', :cuit, '%') ESCAPE '!' OR " +
                " (:cuitSinGuiones <> '' AND REPLACE(c.cuitCuil, '-', '') LIKE CONCAT('%', :cuitSinGuiones, '%') ESCAPE '!'))",
        countQuery = "SELECT COUNT(c.id) FROM Cliente c WHERE c.fechaBaja IS NULL AND " +
                "(:razonSocial = '' OR LOWER(c.denominacion) LIKE LOWER(CONCAT('%', :razonSocial, '%')) ESCAPE '!') AND " +
                "(:cuit = '' OR c.cuitCuil LIKE CONCAT('%', :cuit, '%') ESCAPE '!' OR " +
                " (:cuitSinGuiones <> '' AND REPLACE(c.cuitCuil, '-', '') LIKE CONCAT('%', :cuitSinGuiones, '%') ESCAPE '!'))"
    )
    Page<ClienteDTO> buscarPorCuitYRazonSocial(
            @Param("cuit") String cuit,
            @Param("cuitSinGuiones") String cuitSinGuiones,
            @Param("razonSocial") String razonSocial,
            Pageable pageable);
}
