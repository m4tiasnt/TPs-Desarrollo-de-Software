package com.facturaarca.repository;

import com.facturaarca.entities.ListaPrecioArticulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListaPrecioArticuloRepository extends JpaRepository<ListaPrecioArticulo, Long> {
    List<ListaPrecioArticulo> findByArticuloId(Long articuloId);
    Optional<ListaPrecioArticulo> findFirstByArticuloIdOrderByIdAsc(Long articuloId);
}
