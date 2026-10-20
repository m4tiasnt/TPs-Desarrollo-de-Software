package com.facturaarca.services;

import com.facturaarca.dto.FacturaDetalleRequestDTO;
import com.facturaarca.dto.FacturaReporteDTO;
import com.facturaarca.dto.FacturaRequestDTO;
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
    private final PuntoVentaRepository puntoVentaRepository;
    private final CondicionIvaRepository condicionIvaRepository;
    private final TipoMonedaRepository tipoMonedaRepository;
    private final ClienteRepository clienteRepository;
    private final ListaPrecioArticuloRepository listaPrecioArticuloRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<FacturaReporteDTO> obtenerTodasLasFacturas() {
        return facturaVentaRepository.findAllConRelaciones()
                .stream()
                .map(this::convertirAReporteDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FacturaReporteDTO> buscarFacturasFiltradas(Date fechaDesde, Date fechaHasta, String estado,
            Double montoMinimo) {
        String estadoLimpio = (estado != null && !estado.trim().isEmpty()) ? estado.trim() : null;
        return facturaVentaRepository.buscarFiltradas(fechaDesde, fechaHasta, estadoLimpio, montoMinimo)
                .stream()
                .map(this::convertirAReporteDTO)
                .toList();
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
            Long maxNumero = facturaVentaRepository.obtenerMaxNumero();
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
                if (detDto.getListaPrecioArticuloId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "El listaPrecioArticuloId es obligatorio para cada renglon");
                }

                ListaPrecioArticulo lpa = listaPrecioArticuloRepository.findById(detDto.getListaPrecioArticuloId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "ListaPrecioArticulo no encontrado con id: " + detDto.getListaPrecioArticuloId()));

                FacturaVentaDetalle detalle = FacturaVentaDetalle.crear(
                        lpa,
                        detDto.getDescripcion(),
                        detDto.getCantidad(),
                        detDto.getPorcentajeBonificacion());
                factura.addDetalle(detalle);
            }
        }

        // El dominio recalcula importes netos, IVA, subtotal y total automáticamente
        factura.recalcularTotales();

        // 4. Guardar mediante el repositorio (detalles persisten en cascada)
        FacturaVenta guardada = facturaVentaRepository.save(factura);

        return convertirAReporteDTO(guardada);
    }

    @Transactional
    public FacturaReporteDTO actualizarFactura(Long id, FacturaRequestDTO dto) {
        FacturaVenta factura = facturaVentaRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada con id: " + id));

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

        // Actualizar cabecera delegando al método de dominio
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
                if (detDto.getListaPrecioArticuloId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "El listaPrecioArticuloId es obligatorio para cada renglon");
                }

                ListaPrecioArticulo lpa = listaPrecioArticuloRepository.findById(detDto.getListaPrecioArticuloId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "ListaPrecioArticulo no encontrado con id: " + detDto.getListaPrecioArticuloId()));

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
        FacturaVenta factura = facturaVentaRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada con id: " + id));

        Usuario usuario = obtenerUsuarioParaAuditoria();
        factura.darDeBaja(usuario);
        facturaVentaRepository.save(factura);
    }

    @Transactional
    public FacturaReporteDTO anularFactura(Long id) {
        FacturaVenta factura = facturaVentaRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada con id: " + id));

        Usuario usuario = obtenerUsuarioParaAuditoria();
        factura.anular(usuario);
        FacturaVenta guardada = facturaVentaRepository.save(factura);
        return convertirAReporteDTO(guardada);
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

    private Usuario obtenerUsuarioParaAuditoria() {
        return usuarioRepository.findAll().stream().findFirst().orElse(null);
    }
}
