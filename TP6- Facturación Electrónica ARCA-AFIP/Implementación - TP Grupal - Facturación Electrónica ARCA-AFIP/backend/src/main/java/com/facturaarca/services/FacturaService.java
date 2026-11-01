package com.facturaarca.services;

import com.facturaarca.dto.FacturaVentaDTO;
import com.facturaarca.dto.FacturaVentaRequestDTO;
import com.facturaarca.dto.FacturaVentaDetalleDTO;
import com.facturaarca.dto.FacturaVentaDetalleRequestDTO;
import com.facturaarca.dto.FacturaVentaListadoDTO;
import com.facturaarca.dto.FacturaVentaReporteDTO;
import com.facturaarca.dto.InformarArcaResultadoDTO;
import com.facturaarca.dto.RespuestaPaginadaDTO;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    @Transactional(readOnly = true)
    public List<FacturaVentaReporteDTO> obtenerTodasLasFacturas() {
        return facturaRepository.buscarFacturasFiltradas(null, null, null, null);
    }

    @Transactional(readOnly = true)
    public RespuestaPaginadaDTO<FacturaVentaListadoDTO> buscarFacturasPaginadas(
            Date fechaDesde, Date fechaHasta, EstadoFactura estado, Double montoMinimo,
            Long clienteId, String busqueda, Boolean informadaArca,
            int pagina, int tamanio) {

        Long idBuscado = null;
        Long numeroBuscado = null;
        if (busqueda != null && !busqueda.isBlank()) {
            String limpio = busqueda.trim();
            try {
                if (limpio.startsWith("#")) {
                    idBuscado = Long.parseLong(limpio.substring(1));
                } else {
                    numeroBuscado = Long.parseLong(limpio);
                }
            } catch (NumberFormatException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La búsqueda debe ser un número de factura o un id precedido por #");
            }
        }

        return facturaRepository.buscarFacturasPaginadas(
                fechaDesde, fechaHasta, estado, montoMinimo,
                clienteId, idBuscado, numeroBuscado, informadaArca,
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
        Usuario usuario = obtenerUsuarioParaAuditoria();

        FacturaVenta factura = new FacturaVenta();
        factura.setPuntoVenta(puntoVenta);
        factura.setCondicionIva(condicionIva);
        factura.setTipoMoneda(tipoMoneda);
        factura.setCliente(cliente);
        factura.setEstado(EstadoFactura.BORRADOR);
        factura.setObservaciones(dto.getObservaciones());
        factura.setFechaAlta(new Date());
        factura.setFechaModificacion(new Date());
        if (usuario != null) {
            factura.setUsuarioCarga(usuario);
            factura.setUsuarioModificacion(usuario);
        }

        agregarDetalles(factura, dto.getDetalles());
        factura.recalcularTotales();

        FacturaVenta guardada = facturaVentaRepository.save(factura);
        return convertirAFacturaVentaDTO(guardada);
    }

    @Transactional
    public FacturaVentaDTO actualizarFactura(Long id, FacturaVentaRequestDTO dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los datos de la factura no pueden ser nulos");
        }
        if (dto.getDetalles() != null && dto.getDetalles().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La factura debe tener al menos un detalle");
        }

        FacturaVenta factura = buscarFacturaConDetalles(id);
        if (!factura.getEstado().esEditable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden editar facturas en estado BORRADOR o RECHAZADA");
        }

        Cliente cliente = resolverCliente(dto.getClienteId());
        CondicionIva condicionIva = dto.getCondicionIvaId() != null
                ? resolverCondicionIva(dto.getCondicionIvaId()) : null;
        PuntoVenta puntoVenta = dto.getPuntoVentaId() != null
                ? resolverPuntoVenta(dto.getPuntoVentaId()) : null;
        TipoMoneda tipoMoneda = dto.getTipoMonedaId() != null
                ? resolverTipoMoneda(dto.getTipoMonedaId()) : null;
        Usuario usuario = obtenerUsuarioParaAuditoria();

        factura.actualizarDatos(dto.getObservaciones(), cliente, condicionIva,
                puntoVenta, tipoMoneda, usuario);

        if (dto.getDetalles() != null) {
            factura.clearDetalles();
            agregarDetalles(factura, dto.getDetalles());
            factura.recalcularTotales();
        }

        FacturaVenta guardada = facturaVentaRepository.save(factura);
        return convertirAFacturaVentaDTO(guardada);
    }

    @Transactional
    public void eliminarFactura(Long id) {
        FacturaVenta factura = buscarFactura(id);
        if (!factura.getEstado().esEliminable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden eliminar facturas en estado BORRADOR");
        }
        factura.darDeBaja(obtenerUsuarioParaAuditoria());
        facturaVentaRepository.save(factura);
    }

    @Transactional
    public FacturaVentaDTO anularFactura(Long id) {
        FacturaVenta factura = buscarFacturaConDetalles(id);
        if (!factura.getEstado().esAnulable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede anular una factura en estado " + factura.getEstado());
        }
        factura.anular(obtenerUsuarioParaAuditoria());
        return convertirAFacturaVentaDTO(facturaVentaRepository.save(factura));
    }

    /**
     * Por ahora solo deja la factura EN_PROCESO. La llamada real a ARCA
     * (homologación) se conectará más adelante; ver ROADMAP.md.
     */
    @Transactional
    public List<InformarArcaResultadoDTO> informarArca(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe indicar al menos una factura para informar");
        }

        Usuario usuario = obtenerUsuarioParaAuditoria();
        List<InformarArcaResultadoDTO> resultados = new ArrayList<>();

        for (Long id : ids) {
            FacturaVenta factura = buscarFactura(id);

            if (!factura.getEstado().esInformable()) {
                resultados.add(InformarArcaResultadoDTO.builder()
                        .id(factura.getId())
                        .nroFactura(factura.getNumero())
                        .estado(factura.getEstado())
                        .mensaje("No se puede informar una factura en estado " + factura.getEstado())
                        .build());
                continue;
            }

            factura.marcarEnProceso(usuario);
            FacturaVenta guardada = facturaVentaRepository.save(factura);

            resultados.add(InformarArcaResultadoDTO.builder()
                    .id(guardada.getId())
                    .nroFactura(guardada.getNumero())
                    .estado(guardada.getEstado())
                    .cae(guardada.getCae())
                    .caeFechaVencimiento(guardada.getCaeFechaVencimiento())
                    .fechaEmision(guardada.getFechaEmision())
                    .motivoRechazo(guardada.getMotivoRechazo())
                    .mensaje("Enviada a ARCA. Pendiente de respuesta.")
                    .build());
        }

        return resultados;
    }

    public FacturaVentaDTO convertirAFacturaVentaDTO(FacturaVenta factura) {
        if (factura == null) {
            return null;
        }

        List<FacturaVentaDetalleDTO> detalles = factura.getDetalles() == null
                ? List.of()
                : factura.getDetalles().stream().map(this::convertirAFacturaVentaDetalleDTO).toList();

        double importeNeto = detalles.stream().mapToDouble(FacturaVentaDetalleDTO::getImporteNeto).sum();
        double importeIva = detalles.stream().mapToDouble(FacturaVentaDetalleDTO::getImporteIva).sum();

        return FacturaVentaDTO.builder()
                .id(factura.getId())
                .numero(factura.getNumero())
                .numeroComprobante(formatearNumeroComprobante(factura))
                .fechaEmision(factura.getFechaEmision())
                .clienteId(factura.getCliente() != null ? factura.getCliente().getId() : null)
                .clienteDenominacion(factura.getCliente() != null
                        ? factura.getCliente().getDenominacion() : "Consumidor Final")
                .clienteCuitCuil(factura.getCliente() != null
                        ? factura.getCliente().getCuitCuil() : null)
                .condicionIvaId(factura.getCondicionIva() != null
                        ? factura.getCondicionIva().getId() : null)
                .condicionIva(factura.getCondicionIva() != null
                        ? factura.getCondicionIva().getDenominacion() : "")
                .puntoVentaId(factura.getPuntoVenta() != null
                        ? factura.getPuntoVenta().getId() : null)
                .puntoVentaNumero(factura.getPuntoVenta() != null
                        ? factura.getPuntoVenta().getNumero() : null)
                .puntoVentaDescripcion(factura.getPuntoVenta() != null
                        ? factura.getPuntoVenta().getDescripcion() : "")
                .tipoMonedaId(factura.getTipoMoneda() != null
                        ? factura.getTipoMoneda().getId() : null)
                .tipoMoneda(factura.getTipoMoneda() != null
                        ? factura.getTipoMoneda().getDenominacion() : "")
                .tipoMonedaSimbolo(factura.getTipoMoneda() != null
                        ? factura.getTipoMoneda().getSimbolo() : "")
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
        ListaPrecioArticulo listaPrecioArticulo = detalle.getListaPrecioArticulo();
        Articulo articulo = listaPrecioArticulo != null ? listaPrecioArticulo.getArticulo() : null;
        ListaPrecio listaPrecio = listaPrecioArticulo != null
                ? listaPrecioArticulo.getListaPrecio() : null;

        return FacturaVentaDetalleDTO.builder()
                .id(detalle.getId())
                .articuloId(articulo != null ? articulo.getId() : null)
                .codigoArticulo(articulo != null ? articulo.getCodigo() : null)
                .denominacionArticulo(articulo != null ? articulo.getDenominacion() : null)
                .listaPrecioArticuloId(listaPrecioArticulo != null
                        ? listaPrecioArticulo.getId() : null)
                .listaPrecio(listaPrecio != null ? listaPrecio.getDenominacion() : null)
                .descripcion(detalle.getDescripcion())
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
        for (FacturaVentaDetalleRequestDTO detalleDto : detallesDto) {
            ListaPrecioArticulo listaPrecioArticulo = resolverListaPrecioArticulo(detalleDto);
            FacturaVentaDetalle detalle = FacturaVentaDetalle.crear(
                    listaPrecioArticulo,
                    detalleDto.getDescripcion(),
                    detalleDto.getCantidad(),
                    detalleDto.getPorcentajeBonificacion());
            factura.addDetalle(detalle);
        }
    }

    private void validarDatosObligatorios(FacturaVentaRequestDTO dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los datos de la factura no pueden ser nulos");
        }
        if (dto.getPuntoVentaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El punto de venta es obligatorio");
        }
        if (dto.getCondicionIvaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La condición IVA es obligatoria");
        }
        if (dto.getTipoMonedaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El tipo de moneda es obligatorio");
        }
        if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La factura debe tener al menos un detalle");
        }
    }

    private FacturaVenta buscarFactura(Long id) {
        return facturaVentaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Factura no encontrada con id: " + id));
    }

    private FacturaVenta buscarFacturaConDetalles(Long id) {
        return facturaVentaRepository.buscarConDetalles(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Factura no encontrada con id: " + id));
    }

    private PuntoVenta resolverPuntoVenta(Long id) {
        return puntoVentaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El punto de venta indicado no existe (id: " + id + ")"));
    }

    private CondicionIva resolverCondicionIva(Long id) {
        return condicionIvaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La condición IVA indicada no existe (id: " + id + ")"));
    }

    private TipoMoneda resolverTipoMoneda(Long id) {
        return tipoMonedaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El tipo de moneda indicado no existe (id: " + id + ")"));
    }

    private Cliente resolverCliente(Long id) {
        if (id == null) {
            return null;
        }
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El cliente indicado no existe (id: " + id + ")"));
    }

    private ListaPrecioArticulo resolverListaPrecioArticulo(FacturaVentaDetalleRequestDTO detalleDto) {
        if (detalleDto.getListaPrecioArticuloId() != null) {
            ListaPrecioArticulo lista = listaPrecioArticuloRepository
                    .findById(detalleDto.getListaPrecioArticuloId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La lista de precios del artículo no existe (id: "
                                    + detalleDto.getListaPrecioArticuloId() + ")"));

            if (detalleDto.getArticuloId() != null && lista.getArticulo() != null
                    && !lista.getArticulo().getId().equals(detalleDto.getArticuloId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La lista de precios indicada no pertenece al artículo seleccionado");
            }
            return lista;
        }

        if (detalleDto.getArticuloId() != null) {
            return listaPrecioArticuloRepository
                    .findFirstByArticuloIdOrderByIdAsc(detalleDto.getArticuloId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "El artículo indicado no tiene una lista de precios asociada (id: "
                                    + detalleDto.getArticuloId() + ")"));
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Debe especificar listaPrecioArticuloId o articuloId para cada detalle");
    }

    private Usuario obtenerUsuarioParaAuditoria() {
        return usuarioRepository.findAll().stream().findFirst().orElse(null);
    }

    private static String formatearNumeroComprobante(FacturaVenta factura) {
        if (factura.getPuntoVenta() == null || factura.getNumero() == null) {
            return null;
        }
        return String.format("%04d-%08d", factura.getPuntoVenta().getNumero(), factura.getNumero());
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
