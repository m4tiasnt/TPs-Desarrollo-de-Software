package com.facturaarca.services;

import com.facturaarca.dto.FacturaDetalleRequestDTO;
import com.facturaarca.dto.FacturaReporteDTO;
import com.facturaarca.dto.FacturaRequestDTO;
import com.facturaarca.entities.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FacturaService {

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<FacturaReporteDTO> obtenerTodasLasFacturas() {
        String jpql = """
                    SELECT new com.facturaarca.dto.FacturaReporteDTO(
                        f.numero,
                        f.fechaEmision,
                        COALESCE(c.denominacion, 'Consumidor Final'),
                        ci.denominacion,
                        pv.descripcion,
                        f.importeTotal,
                        COUNT(d)
                    )
                    FROM FacturaVenta f
                    LEFT JOIN f.cliente c
                    JOIN f.condicionIva ci
                    JOIN f.puntoVenta pv
                    JOIN f.detalles d
            GROUP BY f.id,
                     f.numero,
                     f.fechaEmision,
                     c.denominacion,
                     ci.denominacion,
                     pv.descripcion,
                     f.importeTotal
            ORDER BY f.numero
        """;

        TypedQuery<FacturaReporteDTO> query = em.createQuery(jpql, FacturaReporteDTO.class);
        return query.getResultList();
    }

    @Transactional(readOnly = true)
    public List<FacturaReporteDTO> buscarFacturasFiltradas(Date fechaDesde, Date fechaHasta, String estado, Double montoMinimo) {
        StringBuilder jpql = new StringBuilder(
                "SELECT new com.facturaarca.dto.FacturaReporteDTO(" +
                        "f.numero, " +
                        "f.fechaEmision, " +
                        "COALESCE(c.denominacion, 'Consumidor Final'), " +
                        "ci.denominacion, " +
                        "pv.descripcion, " +
                        "f.importeTotal, " +
                        "COUNT(d)" +
                        ") " +
                        "FROM FacturaVenta f " +
                        "LEFT JOIN f.cliente c " +
                        "JOIN f.condicionIva ci " +
                        "JOIN f.puntoVenta pv " +
                        "JOIN f.detalles d " +
                        "WHERE 1=1 "
        );

        Map<String, Object> params = new HashMap<>();

        if (fechaDesde != null) {
            jpql.append("AND f.fechaEmision >= :fechaDesde ");
            params.put("fechaDesde", fechaDesde);
        }
        if (fechaHasta != null) {
            jpql.append("AND f.fechaEmision <= :fechaHasta ");
            params.put("fechaHasta", fechaHasta);
        }
        if (estado != null && !estado.trim().isEmpty()) {
            jpql.append("AND f.estado = :estado ");
            params.put("estado", estado);
        }
        if (montoMinimo != null) {
            jpql.append("AND f.importeTotal >= :montoMinimo ");
            params.put("montoMinimo", montoMinimo);
        }

        jpql.append("GROUP BY f.id, f.numero, f.fechaEmision, c.denominacion, ci.denominacion, pv.descripcion, f.importeTotal");
        jpql.append(" ORDER BY f.numero");

        TypedQuery<FacturaReporteDTO> query = em.createQuery(jpql.toString(), FacturaReporteDTO.class);
        params.forEach(query::setParameter);

        return query.getResultList();
    }

    @Transactional
    public FacturaReporteDTO crearFactura(FacturaRequestDTO dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los datos de la factura no pueden ser nulos");
        }

        // 1. Buscar entidades asociadas por ID y validar existencia
        if (dto.getPuntoVentaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El puntoVentaId es obligatorio");
        }
        PuntoVenta puntoVenta = em.find(PuntoVenta.class, dto.getPuntoVentaId());
        if (puntoVenta == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Punto de Venta no encontrado con id: " + dto.getPuntoVentaId());
        }

        if (dto.getCondicionIvaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El condicionIvaId es obligatorio");
        }
        CondicionIva condicionIva = em.find(CondicionIva.class, dto.getCondicionIvaId());
        if (condicionIva == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Condicion IVA no encontrada con id: " + dto.getCondicionIvaId());
        }

        if (dto.getTipoMonedaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipoMonedaId es obligatorio");
        }
        TipoMoneda tipoMoneda = em.find(TipoMoneda.class, dto.getTipoMonedaId());
        if (tipoMoneda == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de Moneda no encontrado con id: " + dto.getTipoMonedaId());
        }

        if (dto.getClienteId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El clienteId es obligatorio");
        }
        Cliente cliente = em.find(Cliente.class, dto.getClienteId());
        if (cliente == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado con id: " + dto.getClienteId());
        }

        // Obtener un usuario para los campos de auditoría
        Usuario usuario = em.createQuery("SELECT u FROM Usuario u", Usuario.class)
                .setMaxResults(1)
                .getResultList()
                .stream().findFirst().orElse(null);

        // 2. Mapear FacturaRequestDTO a FacturaVenta
        FacturaVenta factura = new FacturaVenta();

        if (dto.getNumero() != null) {
            factura.setNumero(dto.getNumero());
        } else {
            Long maxNumero = em.createQuery("SELECT COALESCE(MAX(f.numero), 0) FROM FacturaVenta f", Long.class)
                    .getSingleResult();
            factura.setNumero(maxNumero + 1L);
        }

        factura.setFechaEmision(dto.getFechaEmision() != null ? dto.getFechaEmision() : new Date());
        factura.setPuntoVenta(puntoVenta);
        factura.setCondicionIva(condicionIva);
        factura.setTipoMoneda(tipoMoneda);
        factura.setCliente(cliente);
        factura.setEstado(dto.getEstado() != null && !dto.getEstado().trim().isEmpty() ? dto.getEstado() : "EMITIDA");
        factura.setObservaciones(dto.getObservaciones());

        factura.setFechaAlta(new Date());
        factura.setFechaModificacion(new Date());
        if (usuario != null) {
            factura.setUsuarioCarga(usuario);
            factura.setUsuarioModificacion(usuario);
        }

        // 3. Mapear y calcular importes automáticamente para los detalles
        double totalCalculado = 0.0;
        if (dto.getDetalles() != null && !dto.getDetalles().isEmpty()) {
            for (FacturaDetalleRequestDTO detDto : dto.getDetalles()) {
                ListaPrecioArticulo lpa = null;
                if (detDto.getListaPrecioArticuloId() != null) {
                    lpa = em.find(ListaPrecioArticulo.class, detDto.getListaPrecioArticuloId());
                } else if (detDto.getArticuloId() != null) {
                    List<ListaPrecioArticulo> precios = em.createQuery(
                                    "SELECT lpa FROM ListaPrecioArticulo lpa WHERE lpa.articulo.id = :artId", ListaPrecioArticulo.class)
                            .setParameter("artId", detDto.getArticuloId())
                            .getResultList();
                    if (!precios.isEmpty()) {
                        lpa = precios.get(0);
                    }
                }

                if (lpa == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "ListaPrecioArticulo no encontrado para el artículo (articuloId: " + detDto.getArticuloId()
                                    + ", listaPrecioArticuloId: " + detDto.getListaPrecioArticuloId() + ")");
                }

                FacturaVentaDetalle detalle = new FacturaVentaDetalle();
                detalle.setListaPrecioArticulo(lpa);
                detalle.setDescripcion(detDto.getDescripcion() != null && !detDto.getDescripcion().trim().isEmpty()
                        ? detDto.getDescripcion()
                        : (lpa.getArticulo() != null ? lpa.getArticulo().getDenominacion() : "Detalle"));

                double cantidad = detDto.getCantidad() > 0 ? detDto.getCantidad() : 1.0;
                detalle.setCantidad(cantidad);

                double precioUnitario = (detDto.getPrecioUnitario() != null && detDto.getPrecioUnitario() > 0)
                        ? detDto.getPrecioUnitario()
                        : lpa.getPrecioVenta();
                detalle.setPrecioUnitario(precioUnitario);

                double bonificacion = detDto.getPorcentajeBonificacion() != null ? detDto.getPorcentajeBonificacion() : 0.0;
                detalle.setPorcentajeBonificacion(bonificacion);

                double subtotal = cantidad * precioUnitario * (1.0 - (bonificacion / 100.0));
                double neto = Math.round((subtotal / 1.21) * 100.0) / 100.0;
                double iva = Math.round((subtotal - neto) * 100.0) / 100.0;

                detalle.setImporteNeto(neto);
                detalle.setImporteIva(iva);
                detalle.setImporteSubtotal(subtotal);

                factura.addDetalle(detalle);
                totalCalculado += subtotal;
            }
        }

        factura.setImporteTotal(dto.getImporteTotal() != null && dto.getImporteTotal() > 0 ? dto.getImporteTotal() : totalCalculado);
        factura.setImporteCobrado(dto.getImporteCobrado() != null ? dto.getImporteCobrado() : factura.getImporteTotal());
        factura.setImporteSaldo(dto.getImporteSaldo() != null ? dto.getImporteSaldo() : (factura.getImporteTotal() - factura.getImporteCobrado()));

        // 4. Guardar la factura (los detalles se persisten en cascada mediante CascadeType.ALL)
        em.persist(factura);

        return convertirAReporteDTO(factura);
    }

    @Transactional
    public FacturaReporteDTO actualizarFactura(Long id, FacturaRequestDTO dto) {
        FacturaVenta factura = em.find(FacturaVenta.class, id);
        if (factura == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada con id: " + id);
        }

        if (dto.getEstado() != null && !dto.getEstado().trim().isEmpty()) {
            factura.setEstado(dto.getEstado());
            if ("ANULADA".equalsIgnoreCase(dto.getEstado()) && factura.getFechaAnulacion() == null) {
                factura.setFechaAnulacion(new Date());
            }
        }

        if (dto.getObservaciones() != null) {
            factura.setObservaciones(dto.getObservaciones());
        }

        if (dto.getFechaEmision() != null) {
            factura.setFechaEmision(dto.getFechaEmision());
        }

        if (dto.getClienteId() != null) {
            Cliente cliente = em.find(Cliente.class, dto.getClienteId());
            if (cliente == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado con id: " + dto.getClienteId());
            }
            factura.setCliente(cliente);
        }

        if (dto.getCondicionIvaId() != null) {
            CondicionIva condicionIva = em.find(CondicionIva.class, dto.getCondicionIvaId());
            if (condicionIva == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Condicion IVA no encontrada con id: " + dto.getCondicionIvaId());
            }
            factura.setCondicionIva(condicionIva);
        }

        if (dto.getPuntoVentaId() != null) {
            PuntoVenta puntoVenta = em.find(PuntoVenta.class, dto.getPuntoVentaId());
            if (puntoVenta == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Punto de Venta no encontrado con id: " + dto.getPuntoVentaId());
            }
            factura.setPuntoVenta(puntoVenta);
        }

        if (dto.getTipoMonedaId() != null) {
            TipoMoneda tipoMoneda = em.find(TipoMoneda.class, dto.getTipoMonedaId());
            if (tipoMoneda == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de Moneda no encontrado con id: " + dto.getTipoMonedaId());
            }
            factura.setTipoMoneda(tipoMoneda);
        }

        // Si se proveen nuevos ítems/detalles, actualizar
        if (dto.getDetalles() != null && !dto.getDetalles().isEmpty()) {
            factura.getDetalles().clear();
            double totalCalculado = 0.0;
            for (FacturaDetalleRequestDTO detDto : dto.getDetalles()) {
                ListaPrecioArticulo lpa = null;
                if (detDto.getListaPrecioArticuloId() != null) {
                    lpa = em.find(ListaPrecioArticulo.class, detDto.getListaPrecioArticuloId());
                } else if (detDto.getArticuloId() != null) {
                    List<ListaPrecioArticulo> precios = em.createQuery(
                                    "SELECT lpa FROM ListaPrecioArticulo lpa WHERE lpa.articulo.id = :artId", ListaPrecioArticulo.class)
                            .setParameter("artId", detDto.getArticuloId())
                            .getResultList();
                    if (!precios.isEmpty()) {
                        lpa = precios.get(0);
                    }
                }

                if (lpa == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "ListaPrecioArticulo no encontrado para el artículo (articuloId: " + detDto.getArticuloId() + ")");
                }

                FacturaVentaDetalle detalle = new FacturaVentaDetalle();
                detalle.setListaPrecioArticulo(lpa);
                detalle.setDescripcion(detDto.getDescripcion() != null && !detDto.getDescripcion().trim().isEmpty()
                        ? detDto.getDescripcion()
                        : (lpa.getArticulo() != null ? lpa.getArticulo().getDenominacion() : "Detalle"));

                double cantidad = detDto.getCantidad() > 0 ? detDto.getCantidad() : 1.0;
                detalle.setCantidad(cantidad);

                double precioUnitario = (detDto.getPrecioUnitario() != null && detDto.getPrecioUnitario() > 0)
                        ? detDto.getPrecioUnitario()
                        : lpa.getPrecioVenta();
                detalle.setPrecioUnitario(precioUnitario);

                double bonificacion = detDto.getPorcentajeBonificacion() != null ? detDto.getPorcentajeBonificacion() : 0.0;
                detalle.setPorcentajeBonificacion(bonificacion);

                double subtotal = cantidad * precioUnitario * (1.0 - (bonificacion / 100.0));
                double neto = Math.round((subtotal / 1.21) * 100.0) / 100.0;
                double iva = Math.round((subtotal - neto) * 100.0) / 100.0;

                detalle.setImporteNeto(neto);
                detalle.setImporteIva(iva);
                detalle.setImporteSubtotal(subtotal);

                factura.addDetalle(detalle);
                totalCalculado += subtotal;
            }
            factura.setImporteTotal(totalCalculado);
        }

        factura.setFechaModificacion(new Date());
        em.merge(factura);

        return convertirAReporteDTO(factura);
    }

    @Transactional
    public void eliminarFactura(Long id) {
        FacturaVenta factura = em.find(FacturaVenta.class, id);
        if (factura == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada con id: " + id);
        }
        em.remove(factura);
    }

    @Transactional
    public FacturaReporteDTO anularFactura(Long id) {
        FacturaVenta factura = em.find(FacturaVenta.class, id);
        if (factura == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada con id: " + id);
        }
        factura.setEstado("ANULADA");
        factura.setFechaAnulacion(new Date());
        factura.setFechaModificacion(new Date());
        em.merge(factura);
        return convertirAReporteDTO(factura);
    }

    public FacturaReporteDTO convertirAReporteDTO(FacturaVenta f) {
        if (f == null) return null;
        return FacturaReporteDTO.builder()
                .id(f.getId())
                .numeroFactura(f.getNumero())
                .fechaEmision(f.getFechaEmision())
                .clienteDenominacion(f.getCliente() != null ? f.getCliente().getDenominacion() : "Consumidor Final")
                .condicionIva(f.getCondicionIva() != null ? f.getCondicionIva().getDenominacion() : "")
                .puntoVentaDescripcion(f.getPuntoVenta() != null ? f.getPuntoVenta().getDescripcion() : "")
                .importeTotal(f.getImporteTotal())
                .cantidadItems((long) (f.getDetalles() != null ? f.getDetalles().size() : 0))
                .estado(f.getEstado())
                .build();
    }
}