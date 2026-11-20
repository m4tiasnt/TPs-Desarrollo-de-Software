package com.facturaarca.repository;

import com.facturaarca.dto.FacturaVentaListadoDTO;
import com.facturaarca.dto.FacturaVentaReporteDTO;
import com.facturaarca.dto.RespuestaPaginadaDTO;
import com.facturaarca.enums.EstadoFactura;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.ZoneId;
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
    public List<FacturaVentaReporteDTO> buscarFacturasFiltradas(
            LocalDate fechaDesde, LocalDate fechaHasta, EstadoFactura estado, Double montoMinimo,
            Long clienteId, Long idBuscado, Integer puntoVentaBuscado, Long numeroBuscado,
            boolean soloSinNumero, Boolean informadaArca) {
        StringBuilder jpql = buildSelectJpql(FacturaVentaReporteDTO.class.getName());
        Map<String, Object> parametros = new HashMap<>();
        appendFiltros(jpql, parametros, fechaDesde, fechaHasta, estado, montoMinimo,
                clienteId, idBuscado, puntoVentaBuscado, numeroBuscado, soloSinNumero, informadaArca);
        appendGroupByOrderBy(jpql);

        TypedQuery<FacturaVentaReporteDTO> query = em.createQuery(jpql.toString(), FacturaVentaReporteDTO.class);
        parametros.forEach(query::setParameter);
        return query.getResultList();
    }

    /**
     * Versión paginada para el listado de la UI.
     */
    public RespuestaPaginadaDTO<FacturaVentaListadoDTO> buscarFacturasPaginadas(
            LocalDate fechaDesde, LocalDate fechaHasta, EstadoFactura estado, Double montoMinimo,
            Long clienteId, Long idBuscado, Integer puntoVentaBuscado, Long numeroBuscado,
            boolean soloSinNumero, Boolean informadaArca,
            int pagina, int tamanio) {

        int paginaSegura = Math.max(0, pagina);
        int tamanioSeguro = Math.max(1, tamanio);
        Map<String, Object> parametros = new HashMap<>();

        StringBuilder countJpql = new StringBuilder(
                "SELECT COUNT(f.id) FROM FacturaVenta f WHERE f.fechaBaja IS NULL ");
        appendFiltros(countJpql, parametros, fechaDesde, fechaHasta, estado, montoMinimo,
                clienteId, idBuscado, puntoVentaBuscado, numeroBuscado, soloSinNumero, informadaArca);

        TypedQuery<Long> countQuery = em.createQuery(countJpql.toString(), Long.class);
        parametros.forEach(countQuery::setParameter);
        long totalElementos = countQuery.getSingleResult();

        parametros.clear();
        StringBuilder jpql = buildSelectJpql(FacturaVentaListadoDTO.class.getName());
        appendFiltros(jpql, parametros, fechaDesde, fechaHasta, estado, montoMinimo,
                clienteId, idBuscado, puntoVentaBuscado, numeroBuscado, soloSinNumero, informadaArca);
        appendGroupByOrderBy(jpql);

        TypedQuery<FacturaVentaListadoDTO> query = em.createQuery(jpql.toString(), FacturaVentaListadoDTO.class);
        parametros.forEach(query::setParameter);
        query.setFirstResult(paginaSegura * tamanioSeguro);
        query.setMaxResults(tamanioSeguro);

        List<FacturaVentaListadoDTO> contenido = query.getResultList();
        int totalPaginas = (int) Math.ceil((double) totalElementos / tamanioSeguro);

        return RespuestaPaginadaDTO.<FacturaVentaListadoDTO>builder()
                .contenido(contenido)
                .paginaActual(paginaSegura)
                .tamanioPagina(tamanioSeguro)
                .totalElementos(totalElementos)
                .totalPaginas(totalPaginas)
                .build();
    }

    private StringBuilder buildSelectJpql(String dtoClassName) {
        return new StringBuilder(
                "SELECT new " + dtoClassName + "(" +
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

    private void appendFiltros(StringBuilder jpql, Map<String, Object> parametros,
                               LocalDate fechaDesde, LocalDate fechaHasta, EstadoFactura estado, Double montoMinimo,
                               Long clienteId, Long idBuscado, Integer puntoVentaBuscado, Long numeroBuscado,
                               boolean soloSinNumero, Boolean informadaArca) {
        ZoneId zona = ZoneId.systemDefault();
        if (fechaDesde != null) {
            jpql.append("AND f.fechaEmision >= :fechaDesde ");
            parametros.put("fechaDesde", Date.from(fechaDesde.atStartOfDay(zona).toInstant()));
        }
        if (fechaHasta != null) {
            jpql.append("AND f.fechaEmision < :fechaHastaExclusiva ");
            parametros.put("fechaHastaExclusiva", Date.from(fechaHasta.plusDays(1).atStartOfDay(zona).toInstant()));
        }
        if (estado != null) {
            jpql.append("AND f.estado = :estado ");
            parametros.put("estado", estado);
        }
        if (montoMinimo != null && montoMinimo > 0) {
            jpql.append("AND f.importeTotal >= :montoMinimo ");
            parametros.put("montoMinimo", montoMinimo);
        }
        if (clienteId != null) {
            jpql.append("AND f.cliente.id = :clienteId ");
            parametros.put("clienteId", clienteId);
        }
        if (idBuscado != null) {
            jpql.append("AND f.id = :idBuscado ");
            parametros.put("idBuscado", idBuscado);
        }
        if (puntoVentaBuscado != null) {
            jpql.append("AND f.puntoVenta.numero = :puntoVentaBuscado ");
            parametros.put("puntoVentaBuscado", puntoVentaBuscado);
        }
        if (numeroBuscado != null) {
            jpql.append("AND f.numero = :numeroBuscado ");
            parametros.put("numeroBuscado", numeroBuscado);
        } else if (soloSinNumero) {
            jpql.append("AND f.numero IS NULL ");
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
}
