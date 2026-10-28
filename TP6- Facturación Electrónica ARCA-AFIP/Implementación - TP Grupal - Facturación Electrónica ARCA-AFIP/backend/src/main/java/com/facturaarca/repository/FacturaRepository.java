package com.facturaarca.repository;

import com.facturaarca.dto.FacturaReporteDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class FacturaRepository {

    @PersistenceContext
    private EntityManager em;

    public List<FacturaReporteDTO> buscarFacturasFiltradas(Date fechaDesde, Date fechaHasta, String estado, Double montoMinimo) {
        StringBuilder jpql = new StringBuilder(
                "SELECT new com.facturaarca.dto.FacturaReporteDTO(" +
                "   f.id, f.numero, f.fechaEmision," +
                "   COALESCE(c.denominacion, 'Consumidor Final')," +
                "   ci.denominacion, pv.descripcion, f.importeTotal, COUNT(d), f.estado" +
                ") " +
                "FROM FacturaVenta f " +
                "LEFT JOIN f.cliente c " +
                "JOIN f.condicionIva ci " +
                "JOIN f.puntoVenta pv " +
                "LEFT JOIN f.detalles d " +
                "WHERE f.fechaBaja IS NULL "
        );

        Map<String, Object> parametros = new HashMap<>();

        if (fechaDesde != null) {
            jpql.append("AND f.fechaEmision >= :fechaDesde ");
            parametros.put("fechaDesde", fechaDesde);
        }
        if (fechaHasta != null) {
            jpql.append("AND f.fechaEmision <= :fechaHasta ");
            parametros.put("fechaHasta", finDelDia(fechaHasta));
        }
        if (estado != null && !estado.trim().isEmpty()) {
            jpql.append("AND f.estado = :estado ");
            parametros.put("estado", estado.trim());
        }
        if (montoMinimo != null) {
            jpql.append("AND f.importeTotal >= :montoMinimo ");
            parametros.put("montoMinimo", montoMinimo);
        }

        jpql.append("GROUP BY f.id, f.numero, f.fechaEmision, c.denominacion, " +
                "ci.denominacion, pv.descripcion, f.importeTotal, f.estado " +
                "ORDER BY f.numero");

        TypedQuery<FacturaReporteDTO> query = em.createQuery(jpql.toString(), FacturaReporteDTO.class);
        parametros.forEach(query::setParameter);
        return query.getResultList();
    }

    private static Date finDelDia(Date fecha) {
        Calendar calendario = Calendar.getInstance();
        calendario.setTime(fecha);
        calendario.set(Calendar.HOUR_OF_DAY, 23);
        calendario.set(Calendar.MINUTE, 59);
        calendario.set(Calendar.SECOND, 59);
        calendario.set(Calendar.MILLISECOND, 999);
        return calendario.getTime();
    }
}
