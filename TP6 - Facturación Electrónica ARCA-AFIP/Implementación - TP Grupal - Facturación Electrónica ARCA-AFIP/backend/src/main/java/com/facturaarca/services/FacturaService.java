package com.facturaarca.services;

import com.facturaarca.arca.ResultadoAutorizacionArca;
import com.facturaarca.dto.FacturaVentaDTO;
import com.facturaarca.dto.FacturaVentaDetalleDTO;
import com.facturaarca.dto.FacturaVentaDetalleRequestDTO;
import com.facturaarca.dto.FacturaVentaListadoDTO;
import com.facturaarca.dto.FacturaVentaReporteDTO;
import com.facturaarca.dto.FacturaVentaRequestDTO;
import com.facturaarca.dto.RespuestaPaginadaDTO;
import com.facturaarca.dto.ResultadoAutorizacionDTO;
import com.facturaarca.entities.Articulo;
import com.facturaarca.entities.Cliente;
import com.facturaarca.entities.CondicionIva;
import com.facturaarca.entities.FacturaVenta;
import com.facturaarca.entities.FacturaVentaDetalle;
import com.facturaarca.entities.ListaPrecio;
import com.facturaarca.entities.ListaPrecioArticulo;
import com.facturaarca.entities.PuntoVenta;
import com.facturaarca.entities.TipoMoneda;
import com.facturaarca.entities.Usuario;
import com.facturaarca.enums.EstadoFactura;
import com.facturaarca.repository.ClienteRepository;
import com.facturaarca.repository.CondicionIvaRepository;
import com.facturaarca.repository.FacturaRepository;
import com.facturaarca.repository.FacturaVentaRepository;
import com.facturaarca.repository.ListaPrecioArticuloRepository;
import com.facturaarca.repository.PuntoVentaRepository;
import com.facturaarca.repository.TipoMonedaRepository;
import com.facturaarca.repository.UsuarioRepository;
import com.facturaarca.util.FormatoComprobante;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacturaService {

    private final FacturaVentaRepository facturaVentaRepository;
    private final FacturaRepository facturaRepository;
    private final PuntoVentaRepository puntoVentaRepository;
    private final CondicionIvaRepository condicionIvaRepository;
    private final TipoMonedaRepository tipoMonedaRepository;
    private final ClienteRepository clienteRepository;
    private final ListaPrecioArticuloRepository listaPrecioArticuloRepository;
    private final UsuarioRepository usuarioRepository;
    private final ArcaService arcaService;
    private final PlatformTransactionManager transactionManager;

    private record CriterioBusqueda(Long id, Integer puntoVenta, Long numero, boolean soloSinNumero) {}
    private record ClaveDetalle(Long listaPrecioArticuloId, double bonificacion, String descripcion) {}

    private static void validarFiltros(LocalDate fechaDesde, LocalDate fechaHasta, Double montoMinimo) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha desde no puede ser posterior a la fecha hasta");
        }
        if (montoMinimo != null && (Double.isNaN(montoMinimo) || Double.isInfinite(montoMinimo) || montoMinimo < 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El monto mínimo no puede ser negativo");
        }
    }

    private static CriterioBusqueda parsearCriterioBusqueda(String busqueda) {
        if (busqueda == null || busqueda.isBlank()) {
            return new CriterioBusqueda(null, null, null, false);
        }
        String limpio = busqueda.trim();
        if (limpio.toLowerCase().startsWith("borrador")) {
            limpio = limpio.replaceFirst("(?i)^borrador\\s*#?\\s*", "#");
        }
        try {
            if (limpio.startsWith("#")) {
                return new CriterioBusqueda(Long.parseLong(limpio.substring(1).trim()), null, null, false);
            }
            if (limpio.contains("-")) {
                String[] partes = limpio.split("-", -1);
                if (partes.length != 2 || partes[0].isBlank()) {
                    throw new NumberFormatException("Formato de comprobante inválido");
                }
                Integer pv = Integer.parseInt(partes[0].trim());
                if (!partes[1].isBlank()) {
                    return new CriterioBusqueda(null, pv, Long.parseLong(partes[1].trim()), false);
                }
                return new CriterioBusqueda(null, pv, null, true);
            }
            return new CriterioBusqueda(null, null, Long.parseLong(limpio), false);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La búsqueda debe ser un número de comprobante (ej: 00001-00000042 o 42) o un id precedido por #");
        }
    }

    @Transactional(readOnly = true)
    public List<FacturaVentaReporteDTO> buscarFacturasParaReporte(
            LocalDate fechaDesde, LocalDate fechaHasta, EstadoFactura estado, Double montoMinimo,
            Long clienteId, String busqueda, Boolean informadaArca) {
        validarFiltros(fechaDesde, fechaHasta, montoMinimo);
        CriterioBusqueda c = parsearCriterioBusqueda(busqueda);
        return facturaRepository.buscarFacturasFiltradas(
                fechaDesde, fechaHasta, estado, montoMinimo,
                clienteId, c.id(), c.puntoVenta(), c.numero(), c.soloSinNumero(), informadaArca);
    }

    @Transactional(readOnly = true)
    public RespuestaPaginadaDTO<FacturaVentaListadoDTO> buscarFacturasPaginadas(
            LocalDate fechaDesde, LocalDate fechaHasta, EstadoFactura estado, Double montoMinimo,
            Long clienteId, String busqueda, Boolean informadaArca,
            int pagina, int tamanio) {
        validarFiltros(fechaDesde, fechaHasta, montoMinimo);
        CriterioBusqueda c = parsearCriterioBusqueda(busqueda);

        return facturaRepository.buscarFacturasPaginadas(
                fechaDesde, fechaHasta, estado, montoMinimo,
                clienteId, c.id(), c.puntoVenta(), c.numero(), c.soloSinNumero(), informadaArca,
                pagina, tamanio);
    }

    @Transactional(readOnly = true)
    public FacturaVentaDTO obtenerDetalle(Long id) {
        return convertirAFacturaVentaDTO(buscarFacturaConDetalles(id));
    }

    @Transactional
    public FacturaVentaDTO crearFactura(FacturaVentaRequestDTO dto) {
        validarDatosObligatorios(dto);

        PuntoVenta puntoVenta = resolverPuntoVenta(dto.getPuntoVentaId());
        CondicionIva condicionIva = resolverCondicionIva(dto.getCondicionIvaId());
        TipoMoneda tipoMoneda = resolverTipoMoneda(dto.getTipoMonedaId());
        Cliente cliente = resolverCliente(dto.getClienteId());
        validarClienteSegunCondicionIva(cliente, condicionIva);
        Usuario usuario = obtenerUsuarioParaAuditoria();

        FacturaVenta factura = new FacturaVenta();
        factura.setPuntoVenta(puntoVenta);
        factura.setCondicionIva(condicionIva);
        factura.setTipoMoneda(tipoMoneda);
        factura.setCliente(cliente);
        factura.setEstado(EstadoFactura.BORRADOR);
        factura.setObservaciones(dto.getObservaciones() != null && !dto.getObservaciones().isBlank()
                ? dto.getObservaciones().trim() : null);
        factura.setFechaAlta(new Date());
        factura.setFechaModificacion(new Date());
        if (usuario != null) {
            factura.setUsuarioCarga(usuario);
            factura.setUsuarioModificacion(usuario);
        }

        agregarDetalles(factura, dto.getDetalles());
        factura.recalcularTotales();
        validarImporteTotalPositivo(factura);

        FacturaVenta guardada = facturaVentaRepository.save(factura);
        return convertirAFacturaVentaDTO(guardada);
    }

    @Transactional
    public FacturaVentaDTO actualizarFactura(Long id, FacturaVentaRequestDTO dto) {
        validarDatosObligatorios(dto);

        FacturaVenta factura = buscarFacturaConDetalles(id);
        if (!factura.getEstado().esEditable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden editar facturas en estado Borrador o Rechazada");
        }

        Cliente cliente = resolverCliente(dto.getClienteId());
        CondicionIva condicionIva = resolverCondicionIva(dto.getCondicionIvaId());
        validarClienteSegunCondicionIva(cliente, condicionIva);
        PuntoVenta puntoVenta = resolverPuntoVenta(dto.getPuntoVentaId());
        TipoMoneda tipoMoneda = resolverTipoMoneda(dto.getTipoMonedaId());
        Usuario usuario = obtenerUsuarioParaAuditoria();

        factura.actualizarDatos(dto.getObservaciones(), cliente, condicionIva,
                puntoVenta, tipoMoneda, usuario);

        factura.clearDetalles();
        agregarDetalles(factura, dto.getDetalles());
        factura.recalcularTotales();
        validarImporteTotalPositivo(factura);

        FacturaVenta guardada = facturaVentaRepository.save(factura);
        return convertirAFacturaVentaDTO(guardada);
    }

    @Transactional
    public void eliminarFactura(Long id) {
        FacturaVenta factura = buscarFactura(id);
        if (!factura.getEstado().esEliminable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden eliminar facturas en estado Borrador");
        }
        factura.darDeBaja(obtenerUsuarioParaAuditoria());
        facturaVentaRepository.save(factura);
    }

    @Transactional
    public FacturaVentaDTO anularFactura(Long id) {
        FacturaVenta factura = buscarFacturaConDetalles(id);
        if (!factura.getEstado().esAnulable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede anular una factura en su estado actual");
        }
        factura.anular(obtenerUsuarioParaAuditoria());
        FacturaVenta guardada = facturaVentaRepository.save(factura);
        return convertirAFacturaVentaDTO(guardada);
    }

    private static final List<EstadoFactura> ESTADOS_INFORMABLES =
            List.of(EstadoFactura.BORRADOR, EstadoFactura.RECHAZADA);

    /**
     * Informa una factura individual ante ARCA usando transacciones cortas
     * para confirmar EN_PROCESO antes de la llamada SOAP y persistir el resultado al volver.
     */
    public ResultadoAutorizacionDTO informarArca(Long id) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        FacturaVenta factura = tx.execute(status -> reservarEnProceso(id));
        if (factura == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo preparar la factura");
        }
        return autorizarConArca(factura, tx);
    }

    /**
     * Informa un lote de facturas ante ARCA.
     * Reserva primero todas las facturas válidas en estado EN_PROCESO y luego
     * autoriza cada comprobante en su propia transacción para aislar fallas.
     */
    public List<ResultadoAutorizacionDTO> informarArcaLote(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Long> idsLimpios = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (idsLimpios.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Indicá al menos una factura válida para informar a ARCA");
        }

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Map<Long, FacturaVenta> reservadas = new LinkedHashMap<>();
        Map<Long, ResultadoAutorizacionDTO> resultados = new LinkedHashMap<>();

        tx.executeWithoutResult(status -> {
            for (Long id : idsLimpios) {
                try {
                    reservadas.put(id, reservarEnProceso(id));
                } catch (ResponseStatusException rse) {
                    log.warn("Factura ID {} no pudo reservarse en lote: {}", id, rse.getReason());
                    resultados.put(id, ResultadoAutorizacionDTO.builder()
                            .id(id)
                            .estado(obtenerEstadoActual(id))
                            .motivoRechazo(rse.getReason())
                            .mensaje(rse.getReason())
                            .build());
                }
            }
        });

        for (Long id : idsLimpios) {
            FacturaVenta factura = reservadas.get(id);
            if (factura == null) {
                continue;
            }
            try {
                resultados.put(id, autorizarConArca(factura, tx));
            } catch (Exception ex) {
                log.error("Error al procesar factura ID {} en lote: {}", id, ex.getMessage(), ex);
                String motivo = "No se pudo autorizar el comprobante por un error interno";
                EstadoFactura estadoActual = revertirEnProcesoSiFalla(id, motivo, tx);
                resultados.put(id, ResultadoAutorizacionDTO.builder()
                        .id(id)
                        .estado(estadoActual)
                        .motivoRechazo(motivo)
                        .mensaje(motivo)
                        .build());
            }
        }

        return new ArrayList<>(resultados.values());
    }

    private FacturaVenta reservarEnProceso(Long id) {
        int actualizadas = facturaVentaRepository.marcarEnProceso(
                id, EstadoFactura.EN_PROCESO, ESTADOS_INFORMABLES, new Date(), obtenerUsuarioParaAuditoria());
        FacturaVenta factura = buscarFacturaConDetalles(id);
        if (actualizadas == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La factura no puede informarse en su estado actual (" + factura.getEstado() + ")");
        }
        return factura;
    }

    private ResultadoAutorizacionDTO autorizarConArca(FacturaVenta facturaPreparada, TransactionTemplate tx) {
        Long id = facturaPreparada.getId();
        ResultadoAutorizacionArca resultadoArca;
        try {
            resultadoArca = arcaService.solicitarAutorizacion(facturaPreparada);
        } catch (Exception ex) {
            log.error("Error inesperado al solicitar autorización ARCA para factura {}: {}", id, ex.getMessage(), ex);
            revertirEnProcesoSiFalla(id, "No se pudo completar la comunicación con ARCA", tx);
            throw ex;
        }

        FacturaVenta guardada = tx.execute(status -> {
            FacturaVenta factura = buscarFacturaConDetalles(id);
            Usuario usuario = obtenerUsuarioParaAuditoria();
            if (resultadoArca.isAutorizado()) {
                factura.autorizar(
                        resultadoArca.getNumeroComprobante(),
                        resultadoArca.getFechaEmision(),
                        resultadoArca.getCae(),
                        resultadoArca.getCaeVencimiento(),
                        usuario
                );
            } else {
                factura.rechazar(resultadoArca.getMotivoRechazo(), usuario);
            }
            return facturaVentaRepository.save(factura);
        });

        if (guardada == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el resultado de ARCA");
        }

        return ResultadoAutorizacionDTO.builder()
                .id(guardada.getId())
                .numero(guardada.getNumero())
                .estado(guardada.getEstado())
                .cae(guardada.getCae())
                .caeFechaVencimiento(guardada.getCaeFechaVencimiento())
                .fechaEmision(guardada.getFechaEmision())
                .motivoRechazo(guardada.getMotivoRechazo())
                .mensaje(resultadoArca.getMensaje())
                .build();
    }

    private EstadoFactura obtenerEstadoActual(Long id) {
        return facturaVentaRepository.findById(id)
                .filter(f -> f.getFechaBaja() == null)
                .map(FacturaVenta::getEstado)
                .orElse(null);
    }

    private EstadoFactura revertirEnProcesoSiFalla(Long id, String motivoFallo, TransactionTemplate tx) {
        try {
            return tx.execute(status -> {
                Optional<FacturaVenta> opt = facturaVentaRepository.findById(id)
                        .filter(f -> f.getFechaBaja() == null);
                if (opt.isEmpty()) {
                    return null;
                }
                FacturaVenta f = opt.get();
                if (f.getEstado() == EstadoFactura.EN_PROCESO) {
                    f.rechazar(motivoFallo, obtenerUsuarioParaAuditoria());
                    facturaVentaRepository.save(f);
                }
                return f.getEstado();
            });
        } catch (Exception e) {
            log.warn("No se pudo actualizar el estado de fallback para la factura {}: {}", id, e.getMessage());
            return EstadoFactura.RECHAZADA;
        }
    }

    public FacturaVentaDTO convertirAFacturaVentaDTO(FacturaVenta factura) {
        if (factura == null) {
            return null;
        }

        List<FacturaVentaDetalleDTO> detalles = factura.getDetalles() == null
                ? List.of()
                : factura.getDetalles().stream()
                        .filter(Objects::nonNull)
                        .map(this::convertirAFacturaVentaDetalleDTO)
                        .toList();

        double importeNeto = detalles.stream().mapToDouble(FacturaVentaDetalleDTO::getImporteNeto).sum();
        double importeIva = detalles.stream().mapToDouble(FacturaVentaDetalleDTO::getImporteIva).sum();

        Cliente cliente = factura.getCliente();
        CondicionIva condicionIva = factura.getCondicionIva();
        PuntoVenta puntoVenta = factura.getPuntoVenta();
        TipoMoneda tipoMoneda = factura.getTipoMoneda();

        return FacturaVentaDTO.builder()
                .id(factura.getId())
                .numero(factura.getNumero())
                .numeroComprobante(formatearNumeroComprobante(factura))
                .fechaEmision(factura.getFechaEmision())
                .clienteId(cliente != null ? cliente.getId() : null)
                .clienteDenominacion(cliente != null && cliente.getDenominacion() != null && !cliente.getDenominacion().isBlank()
                        ? cliente.getDenominacion() : "Consumidor Final")
                .clienteCuitCuil(cliente != null ? cliente.getCuitCuil() : null)
                .condicionIvaId(condicionIva != null ? condicionIva.getId() : null)
                .condicionIva(condicionIva != null ? condicionIva.getDenominacion() : "")
                .puntoVentaId(puntoVenta != null ? puntoVenta.getId() : null)
                .puntoVentaNumero(puntoVenta != null ? puntoVenta.getNumero() : null)
                .puntoVentaDescripcion(puntoVenta != null ? puntoVenta.getDescripcion() : "")
                .tipoMonedaId(tipoMoneda != null ? tipoMoneda.getId() : null)
                .tipoMoneda(tipoMoneda != null ? tipoMoneda.getDenominacion() : "")
                .tipoMonedaSimbolo(tipoMoneda != null ? tipoMoneda.getSimbolo() : "")
                .importeNeto(redondear(importeNeto))
                .importeIva(redondear(importeIva))
                .importeTotal(factura.getImporteTotal())
                .importeCobrado(factura.getImporteCobrado())
                .importeSaldo(factura.getImporteSaldo())
                .estado(factura.getEstado())
                .cae(factura.getCae())
                .caeFechaVencimiento(factura.getCaeFechaVencimiento())
                .resultadoAfip(factura.getResultadoAfip())
                .motivoRechazo(factura.getMotivoRechazo())
                .observaciones(factura.getObservaciones())
                .fechaAlta(factura.getFechaAlta())
                .fechaModificacion(factura.getFechaModificacion())
                .fechaAnulacion(factura.getFechaAnulacion())
                .detalles(detalles)
                .build();
    }

    private FacturaVentaDetalleDTO convertirAFacturaVentaDetalleDTO(FacturaVentaDetalle detalle) {
        if (detalle == null) {
            return null;
        }

        ListaPrecioArticulo listaPrecioArticulo = detalle.getListaPrecioArticulo();
        Articulo articulo = listaPrecioArticulo != null ? listaPrecioArticulo.getArticulo() : null;
        ListaPrecio listaPrecio = listaPrecioArticulo != null ? listaPrecioArticulo.getListaPrecio() : null;

        String desc = detalle.getDescripcion();
        if ((desc == null || desc.isBlank()) && articulo != null) {
            desc = articulo.getDenominacion();
        }

        return FacturaVentaDetalleDTO.builder()
                .id(detalle.getId())
                .articuloId(articulo != null ? articulo.getId() : null)
                .codigoArticulo(articulo != null ? articulo.getCodigo() : null)
                .denominacionArticulo(articulo != null ? articulo.getDenominacion() : null)
                .listaPrecioArticuloId(listaPrecioArticulo != null ? listaPrecioArticulo.getId() : null)
                .listaPrecio(listaPrecio != null ? listaPrecio.getDenominacion() : null)
                .descripcion(desc != null ? desc : "Detalle")
                .cantidad(detalle.getCantidad())
                .precioUnitario(detalle.getPrecioUnitario())
                .porcentajeBonificacion(detalle.getPorcentajeBonificacion())
                .importeNeto(detalle.getImporteNeto())
                .importeIva(detalle.getImporteIva())
                .importeSubtotal(detalle.getImporteSubtotal())
                .build();
    }

    private void agregarDetalles(FacturaVenta factura, List<FacturaVentaDetalleRequestDTO> detallesDto) {
        if (detallesDto == null || detallesDto.isEmpty()) {
            return;
        }
        Map<ClaveDetalle, FacturaVentaDetalle> detallesAgrupados = new LinkedHashMap<>();
        for (FacturaVentaDetalleRequestDTO detalleDto : detallesDto) {
            ListaPrecioArticulo listaPrecioArticulo = resolverListaPrecioArticulo(detalleDto);
            Long listaPrecioArticuloId = listaPrecioArticulo.getId();
            double bonif = detalleDto.getPorcentajeBonificacion() != null ? detalleDto.getPorcentajeBonificacion() : 0.0;
            String descNormalizada = detalleDto.getDescripcion() != null ? detalleDto.getDescripcion().trim() : "";

            ClaveDetalle clave = listaPrecioArticuloId != null
                    ? new ClaveDetalle(listaPrecioArticuloId, bonif, descNormalizada)
                    : null;

            FacturaVentaDetalle existente = clave != null ? detallesAgrupados.get(clave) : null;
            if (existente != null) {
                double cantidadExtra = detalleDto.getCantidad() > 0 ? detalleDto.getCantidad() : 1.0;
                existente.setCantidad(existente.getCantidad() + cantidadExtra);
                existente.calcularImportes();
                continue;
            }

            FacturaVentaDetalle detalle = FacturaVentaDetalle.crear(
                    listaPrecioArticulo,
                    descNormalizada.isEmpty() ? null : descNormalizada,
                    detalleDto.getCantidad(),
                    bonif);
            factura.addDetalle(detalle);
            if (clave != null) {
                detallesAgrupados.put(clave, detalle);
            }
        }
    }

    private void validarDatosObligatorios(FacturaVentaRequestDTO dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los datos de la factura son obligatorios");
        }
        if (dto.getPuntoVentaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El punto de venta es obligatorio");
        }
        if (dto.getCondicionIvaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La condición de IVA es obligatoria");
        }
        if (dto.getTipoMonedaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La moneda es obligatoria");
        }
        if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La factura debe tener al menos un artículo");
        }
    }

    private void validarClienteSegunCondicionIva(Cliente cliente, CondicionIva condicionIva) {
        if (cliente != null || condicionIva == null) {
            return;
        }
        String denom = condicionIva.getDenominacion() != null ? condicionIva.getDenominacion().toLowerCase() : "";
        if (!denom.contains("consumidor final")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El cliente es obligatorio para la condición de IVA seleccionada (" + condicionIva.getDenominacion() + ")");
        }
    }

    private void validarImporteTotalPositivo(FacturaVenta factura) {
        if (factura.getImporteTotal() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El importe total de la factura debe ser mayor a cero");
        }
    }

    private FacturaVenta buscarFactura(Long id) {
        return facturaVentaRepository.findById(id)
                .filter(f -> f.getFechaBaja() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La factura solicitada no existe"));
    }

    private FacturaVenta buscarFacturaConDetalles(Long id) {
        return facturaVentaRepository.buscarConDetalles(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La factura solicitada no existe"));
    }

    private PuntoVenta resolverPuntoVenta(Long id) {
        return puntoVentaRepository.findById(id)
                .filter(pv -> pv.getFechaBaja() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El punto de venta seleccionado no existe"));
    }

    private CondicionIva resolverCondicionIva(Long id) {
        return condicionIvaRepository.findById(id)
                .filter(ci -> ci.getFechaBaja() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La condición de IVA seleccionada no existe"));
    }

    private TipoMoneda resolverTipoMoneda(Long id) {
        return tipoMonedaRepository.findById(id)
                .filter(tm -> tm.getFechaBaja() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La moneda seleccionada no existe"));
    }

    private Cliente resolverCliente(Long id) {
        if (id == null) {
            return null;
        }
        return clienteRepository.findById(id)
                .filter(c -> c.getFechaBaja() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El cliente seleccionado no existe"));
    }

    private ListaPrecioArticulo resolverListaPrecioArticulo(FacturaVentaDetalleRequestDTO detalleDto) {
        if (detalleDto.getListaPrecioArticuloId() != null) {
            ListaPrecioArticulo lista = listaPrecioArticuloRepository
                    .findById(detalleDto.getListaPrecioArticuloId())
                    .filter(lpa -> lpa.getFechaBaja() == null)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La lista de precios seleccionada para el artículo no existe"));

            if (detalleDto.getArticuloId() != null && lista.getArticulo() != null
                    && !lista.getArticulo().getId().equals(detalleDto.getArticuloId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La lista de precios indicada no pertenece al artículo seleccionado");
            }
            return lista;
        }

        if (detalleDto.getArticuloId() != null) {
            return listaPrecioArticuloRepository
                    .findFirstByArticuloIdAndFechaBajaIsNullOrderByIdAsc(detalleDto.getArticuloId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "El artículo seleccionado no tiene una lista de precios vigente"));
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Indicá un artículo y una lista de precios válida para cada detalle");
    }

    private Usuario obtenerUsuarioParaAuditoria() {
        return usuarioRepository.findById(1L)
                .orElseGet(() -> usuarioRepository.findAll().stream().findFirst().orElse(null));
    }

    private static String formatearNumeroComprobante(FacturaVenta factura) {
        if (factura == null || factura.getNumero() == null) {
            return null;
        }
        int ptoVta = (factura.getPuntoVenta() != null) ? factura.getPuntoVenta().getNumero() : 1;
        return FormatoComprobante.formatearNumeroComprobante(ptoVta, factura.getNumero());
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
