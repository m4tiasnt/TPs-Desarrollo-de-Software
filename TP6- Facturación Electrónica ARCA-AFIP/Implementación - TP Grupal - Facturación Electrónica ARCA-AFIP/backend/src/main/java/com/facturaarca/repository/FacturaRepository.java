package com.facturaarca.repository;

import com.facturaarca.dto.FacturaReporteDTO;
import com.facturaarca.dto.RespuestaPaginadaDTO;
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
     * Versión sin paginación (devuelve todo). Se conserva para el
     * ReporteService que exporta PDF/Excel con todas las facturas.
     */
    public List<FacturaReporteDTO> buscarFacturasFiltradas(Date fechaDesde, Date fechaHasta, String estado, Double montoMinimo) {
        StringBuilder jpql = buildSelectJpql();
        Map<String, Object> parametros = new HashMap<>();
        appendFiltros(jpql, parametros, fechaDesde, fechaHasta, estado, montoMinimo);
        appendGroupByOrderBy(jpql);

        TypedQuery<FacturaReporteDTO> query = em.createQuery(jpql.toString(), FacturaReporteDTO.class);
        parametros.forEach(query::setParameter);
        return query.getResultList();
    }

    /**
     * Versión con paginación server-side.
     * Ejecuta una COUNT query paralela para saber el total y aplica
     * LIMIT/OFFSET con setFirstResult / setMaxResults.
     *
     * @param pagina página actual (0-indexed)
     * @param tamanio cantidad de registros por página
     */
    public RespuestaPaginadaDTO<FacturaReporteDTO> buscarFacturasPaginadas(
            Date fechaDesde, Date fechaHasta, String estado, Double montoMinimo,
            int pagina, int tamanio) {

        Map<String, Object> parametros = new HashMap<>();

        // 1. COUNT query — cuenta las facturas que cumplen los filtros.
        // No requiere ningún JOIN ya que los filtros son atributos directos de FacturaVenta.
        // Esto asegura que facturas a consumidor final (sin cliente) o sin detalles se cuenten fielmente.
        StringBuilder countJpql = new StringBuilder(
                "SELECT COUNT(f.id) " +
                "FROM FacturaVenta f " +
                "WHERE f.fechaBaja IS NULL "
        );
        appendFiltros(countJpql, parametros, fechaDesde, fechaHasta, estado, montoMinimo);

        TypedQuery<Long> countQuery = em.createQuery(countJpql.toString(), Long.class);
        parametros.forEach(countQuery::setParameter);
        long totalElementos = countQuery.getSingleResult();

        // 2. SELECT query — trae solo la página pedida
        parametros.clear(); // los volvemos a poblar con appendFiltros
        StringBuilder jpql = buildSelectJpql();
        appendFiltros(jpql, parametros, fechaDesde, fechaHasta, estado, montoMinimo);
        appendGroupByOrderBy(jpql);

        TypedQuery<FacturaReporteDTO> query = em.createQuery(jpql.toString(), FacturaReporteDTO.class);
        parametros.forEach(query::setParameter);
        query.setFirstResult(pagina * tamanio);
        query.setMaxResults(tamanio);

        List<FacturaReporteDTO> contenido = query.getResultList();
        int totalPaginas = (int) Math.ceil((double) totalElementos / tamanio);

        return RespuestaPaginadaDTO.<FacturaReporteDTO>builder()
                .contenido(contenido)
                .paginaActual(pagina)
                .tamanioPagina(tamanio)
                .totalElementos(totalElementos)
                .totalPaginas(totalPaginas)
                .build();
    }

    // === Helpers privados para no duplicar la lógica de armado de JPQL ===

    private StringBuilder buildSelectJpql() {
        return new StringBuilder(
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
    }

    private void appendFiltros(StringBuilder jpql, Map<String, Object> parametros,
                               Date fechaDesde, Date fechaHasta, String estado, Double montoMinimo) {
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
    }

    private void appendGroupByOrderBy(StringBuilder jpql) {
        jpql.append("GROUP BY f.id, f.numero, f.fechaEmision, c.denominacion, " +
                "ci.denominacion, pv.descripcion, f.importeTotal, f.estado " +
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
