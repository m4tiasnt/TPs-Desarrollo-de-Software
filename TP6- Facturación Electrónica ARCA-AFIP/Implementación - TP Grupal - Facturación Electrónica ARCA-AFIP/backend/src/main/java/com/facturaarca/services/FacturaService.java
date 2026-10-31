package com.facturaarca.services;

import com.facturaarca.dto.FacturaDetalleRequestDTO;
import com.facturaarca.dto.FacturaReporteDTO;
import com.facturaarca.dto.FacturaRequestDTO;
import com.facturaarca.dto.RespuestaPaginadaDTO;
import com.facturaarca.entities.*;
import com.facturaarca.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    public List<FacturaReporteDTO> obtenerTodasLasFacturas() {
        return buscarFacturasFiltradas(null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<FacturaReporteDTO> buscarFacturasFiltradas(Date fechaDesde, Date fechaHasta, String estado, Double montoMinimo) {
        return facturaRepository.buscarFacturasFiltradas(fechaDesde, fechaHasta, estado, montoMinimo);
    }

    @Transactional(readOnly = true)
    public RespuestaPaginadaDTO<FacturaReporteDTO> buscarFacturasPaginadas(
            Date fechaDesde, Date fechaHasta, String estado, Double montoMinimo,
            int pagina, int tamanio) {
        return facturaRepository.buscarFacturasPaginadas(fechaDesde, fechaHasta, estado, montoMinimo, pagina, tamanio);
    }

    @Transactional
    public FacturaReporteDTO crearFactura(FacturaRequestDTO dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los datos de la factura no pueden ser nulos");
        }

        // 1. Buscar y validar entidades asociadas mediante sus repositorios
        if (dto.getPuntoVentaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El puntoVentaId es obligatorio");
        }
        PuntoVenta puntoVenta = puntoVentaRepository.findById(dto.getPuntoVentaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Punto de Venta no encontrado con id: " + dto.getPuntoVentaId()));

        if (dto.getCondicionIvaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El condicionIvaId es obligatorio");
        }
        CondicionIva condicionIva = condicionIvaRepository.findById(dto.getCondicionIvaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Condicion IVA no encontrada con id: " + dto.getCondicionIvaId()));

        if (dto.getTipoMonedaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipoMonedaId es obligatorio");
        }
        TipoMoneda tipoMoneda = tipoMonedaRepository.findById(dto.getTipoMonedaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tipo de Moneda no encontrado con id: " + dto.getTipoMonedaId()));

        Cliente cliente = null;
        if (dto.getClienteId() != null) {
            cliente = clienteRepository.findById(dto.getClienteId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Cliente no encontrado con id: " + dto.getClienteId()));
        }

        Usuario usuario = obtenerUsuarioParaAuditoria();

        // 2. Instanciar y configurar FacturaVenta
        FacturaVenta factura = new FacturaVenta();

        if (dto.getNumero() != null) {
            factura.setNumero(dto.getNumero());
        } else {
            // La numeración es correlativa por punto de venta
            Long maxNumero = facturaVentaRepository.obtenerMaxNumeroPorPuntoVenta(puntoVenta.getId());
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

        // 3. Crear detalles y delegar el cálculo de importes al dominio
        if (dto.getDetalles() != null && !dto.getDetalles().isEmpty()) {
            for (FacturaDetalleRequestDTO detDto : dto.getDetalles()) {
                ListaPrecioArticulo lpa = resolverListaPrecioArticulo(detDto);

                FacturaVentaDetalle detalle = FacturaVentaDetalle.crear(
                        lpa,
                        detDto.getDescripcion(),
                        detDto.getCantidad(),
                        detDto.getPorcentajeBonificacion());
                factura.addDetalle(detalle);
            }
        }

        // Se recalculan importes netos, IVA, subtotal y total automáticamente
        factura.recalcularTotales();

        // 4. Guardar factura
        FacturaVenta guardada = facturaVentaRepository.save(factura);

        return convertirAReporteDTO(guardada);
    }

    @Transactional
    public FacturaReporteDTO actualizarFactura(Long id, FacturaRequestDTO dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los datos de la factura no pueden ser nulos");
        }

        FacturaVenta factura = buscarFactura(id);

        Cliente cliente = null;
        if (dto.getClienteId() != null) {
            cliente = clienteRepository.findById(dto.getClienteId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Cliente no encontrado con id: " + dto.getClienteId()));
        }

        CondicionIva condicionIva = null;
        if (dto.getCondicionIvaId() != null) {
            condicionIva = condicionIvaRepository.findById(dto.getCondicionIvaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Condicion IVA no encontrada con id: " + dto.getCondicionIvaId()));
        }

        PuntoVenta puntoVenta = null;
        if (dto.getPuntoVentaId() != null) {
            puntoVenta = puntoVentaRepository.findById(dto.getPuntoVentaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Punto de Venta no encontrado con id: " + dto.getPuntoVentaId()));
        }

        TipoMoneda tipoMoneda = null;
        if (dto.getTipoMonedaId() != null) {
            tipoMoneda = tipoMonedaRepository.findById(dto.getTipoMonedaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Tipo de Moneda no encontrado con id: " + dto.getTipoMonedaId()));
        }

        Usuario usuario = obtenerUsuarioParaAuditoria();

        // Actualizar cabecera
        factura.actualizarDatos(
                dto.getEstado(),
                dto.getObservaciones(),
                dto.getFechaEmision(),
                cliente,
                condicionIva,
                puntoVenta,
                tipoMoneda,
                usuario);

        // Si se proveen nuevos renglones/detalles, actualizar
        if (dto.getDetalles() != null && !dto.getDetalles().isEmpty()) {
            factura.clearDetalles();

            for (FacturaDetalleRequestDTO detDto : dto.getDetalles()) {
                ListaPrecioArticulo lpa = resolverListaPrecioArticulo(detDto);

                FacturaVentaDetalle detalle = FacturaVentaDetalle.crear(
                        lpa,
                        detDto.getDescripcion(),
                        detDto.getCantidad(),
                        detDto.getPorcentajeBonificacion());
                factura.addDetalle(detalle);
            }
            factura.recalcularTotales();
        }

        FacturaVenta guardada = facturaVentaRepository.save(factura);
        return convertirAReporteDTO(guardada);
    }

    @Transactional
    public void eliminarFactura(Long id) {
        // Hacemos baja lógica, le  seteamos la fecha_baja a la factura para que deje de figurar
        // en el listado pero toda su información se conserva igual
        FacturaVenta factura = buscarFactura(id);
        factura.darDeBaja(obtenerUsuarioParaAuditoria());
        facturaVentaRepository.save(factura);
    }

    public FacturaReporteDTO convertirAReporteDTO(FacturaVenta f) {
        if (f == null) {
            return null;
        }
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

    private FacturaVenta buscarFactura(Long id) {
        return facturaVentaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Factura no encontrada con id: " + id));
    }

    private ListaPrecioArticulo resolverListaPrecioArticulo(FacturaDetalleRequestDTO detDto) {
        if (detDto.getListaPrecioArticuloId() != null) {
            ListaPrecioArticulo lista = listaPrecioArticuloRepository.findById(detDto.getListaPrecioArticuloId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "ListaPrecioArticulo no encontrado con id: " + detDto.getListaPrecioArticuloId()));

            // La lista de precios elegida tiene que pertenecer al artículo del renglón.
            if (detDto.getArticuloId() != null && lista.getArticulo() != null
                    && !lista.getArticulo().getId().equals(detDto.getArticuloId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La lista de precios " + lista.getId() + " no pertenece al artículo " + detDto.getArticuloId());
            }
            return lista;
        } else if (detDto.getArticuloId() != null) {
            return listaPrecioArticuloRepository.findFirstByArticuloIdOrderByIdAsc(detDto.getArticuloId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "ListaPrecioArticulo no encontrado para el artículo id: " + detDto.getArticuloId()));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe especificar listaPrecioArticuloId o articuloId para cada renglon");
        }
    }

    private Usuario obtenerUsuarioParaAuditoria() {
        return usuarioRepository.findAll().stream().findFirst().orElse(null);
    }
}
