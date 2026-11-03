package com.facturaarca.repository;

import com.facturaarca.dto.FacturaVentaListadoDTO;
import com.facturaarca.dto.FacturaVentaReporteDTO;
import com.facturaarca.dto.RespuestaPaginadaDTO;
import com.facturaarca.enums.EstadoFactura;
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

    /**
     * Versión sin paginación para los reportes PDF/Excel.
     */
    public List<FacturaVentaReporteDTO> buscarFacturasFiltradas(Date fechaDesde, Date fechaHasta,
                                                           EstadoFactura estado, Double montoMinimo) {
        StringBuilder jpql = buildSelectReporteJpql();
        Map<String, Object> parametros = new HashMap<>();
        appendFiltros(jpql, parametros, fechaDesde, fechaHasta, estado, montoMinimo,
                null, null, null, null);
        appendGroupByOrderBy(jpql);

        TypedQuery<FacturaVentaReporteDTO> query = em.createQuery(jpql.toString(), FacturaVentaReporteDTO.class);
        parametros.forEach(query::setParameter);
        return query.getResultList();
    }

    /**
     * Versión paginada para el listado de la UI.
     */
    public RespuestaPaginadaDTO<FacturaVentaListadoDTO> buscarFacturasPaginadas(
            Date fechaDesde, Date fechaHasta, EstadoFactura estado, Double montoMinimo,
            Long clienteId, Long idBuscado, Long numeroBuscado, Boolean informadaArca,
            int pagina, int tamanio) {

        Map<String, Object> parametros = new HashMap<>();

        StringBuilder countJpql = new StringBuilder(
                "SELECT COUNT(f.id) FROM FacturaVenta f WHERE f.fechaBaja IS NULL ");
        appendFiltros(countJpql, parametros, fechaDesde, fechaHasta, estado, montoMinimo,
                clienteId, idBuscado, numeroBuscado, informadaArca);

        TypedQuery<Long> countQuery = em.createQuery(countJpql.toString(), Long.class);
        parametros.forEach(countQuery::setParameter);
        long totalElementos = countQuery.getSingleResult();

        parametros.clear();
        StringBuilder jpql = buildSelectListadoJpql();
        appendFiltros(jpql, parametros, fechaDesde, fechaHasta, estado, montoMinimo,
                clienteId, idBuscado, numeroBuscado, informadaArca);
        appendGroupByOrderBy(jpql);

        TypedQuery<FacturaVentaListadoDTO> query = em.createQuery(jpql.toString(), FacturaVentaListadoDTO.class);
        parametros.forEach(query::setParameter);
        query.setFirstResult(pagina * tamanio);
        query.setMaxResults(tamanio);

        List<FacturaVentaListadoDTO> contenido = query.getResultList();
        int totalPaginas = (int) Math.ceil((double) totalElementos / tamanio);

        return RespuestaPaginadaDTO.<FacturaVentaListadoDTO>builder()
                .contenido(contenido)
                .paginaActual(pagina)
                .tamanioPagina(tamanio)
                .totalElementos(totalElementos)
                .totalPaginas(totalPaginas)
                .build();
    }

    private StringBuilder buildSelectListadoJpql() {
        return new StringBuilder(
                "SELECT new com.facturaarca.dto.FacturaVentaListadoDTO(" +
                "   f.id, f.numero, f.fechaEmision," +
                "   COALESCE(c.denominacion, 'Consumidor Final')," +
                "   ci.denominacion, pv.numero, pv.descripcion, f.importeTotal, COUNT(d), f.estado" +
                ") " +
                "FROM FacturaVenta f " +
                "LEFT JOIN f.cliente c " +
                "JOIN f.condicionIva ci " +
                "JOIN f.puntoVenta pv " +
                "LEFT JOIN f.detalles d " +
                "WHERE f.fechaBaja IS NULL "
        );
    }

    private StringBuilder buildSelectReporteJpql() {
        return new StringBuilder(
                "SELECT new com.facturaarca.dto.FacturaVentaReporteDTO(" +
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
    }

    private void appendFiltros(StringBuilder jpql, Map<String, Object> parametros,
                               Date fechaDesde, Date fechaHasta, EstadoFactura estado, Double montoMinimo,
                               Long clienteId, Long idBuscado, Long numeroBuscado, Boolean informadaArca) {
        if (fechaDesde != null) {
            jpql.append("AND f.fechaEmision >= :fechaDesde ");
            parametros.put("fechaDesde", fechaDesde);
        }
        if (fechaHasta != null) {
            jpql.append("AND f.fechaEmision <= :fechaHasta ");
            parametros.put("fechaHasta", finDelDia(fechaHasta));
        }
        if (estado != null) {
            jpql.append("AND f.estado = :estado ");
            parametros.put("estado", estado);
        }
        if (montoMinimo != null && montoMinimo > 0) {
            jpql.append("AND f.importeTotal >= :montoMinimo ");
            parametros.put("montoMinimo", montoMinimo);
        }
        if (clienteId != null && clienteId > 0) {
            jpql.append("AND f.cliente.id = :clienteId ");
            parametros.put("clienteId", clienteId);
        }
        if (idBuscado != null) {
            jpql.append("AND f.id = :idBuscado ");
            parametros.put("idBuscado", idBuscado);
        }
        if (numeroBuscado != null) {
            jpql.append("AND f.numero = :numeroBuscado ");
            parametros.put("numeroBuscado", numeroBuscado);
        }
        if (informadaArca != null) {
            if (informadaArca) {
                jpql.append("AND f.estado = :estadoInformada ");
            } else {
                jpql.append("AND f.estado <> :estadoInformada ");
            }
            parametros.put("estadoInformada", EstadoFactura.AUTORIZADA);
        }
    }

    private void appendGroupByOrderBy(StringBuilder jpql) {
        jpql.append("GROUP BY f.id, f.numero, f.fechaEmision, c.denominacion, " +
                "ci.denominacion, pv.numero, pv.descripcion, f.importeTotal, f.estado " +
                "ORDER BY f.id DESC");
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
