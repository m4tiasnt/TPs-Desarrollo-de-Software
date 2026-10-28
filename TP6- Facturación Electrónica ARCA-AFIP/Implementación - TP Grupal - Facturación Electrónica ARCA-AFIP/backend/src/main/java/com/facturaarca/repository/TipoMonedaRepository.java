package com.facturaarca.repository;

import com.facturaarca.entities.TipoMoneda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoMonedaRepository extends JpaRepository<TipoMoneda, Long> {
}
