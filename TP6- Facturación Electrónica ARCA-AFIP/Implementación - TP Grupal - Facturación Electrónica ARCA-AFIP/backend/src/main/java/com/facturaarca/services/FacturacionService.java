package com.facturaarca.services;

import com.facturaarca.AfipWsaaClient;
import com.facturaarca.AfipWsfeClient;
import com.facturaarca.dto.FacturaVentaDTO;
import com.facturaarca.dto.FacturaVentaDetalleRequestDTO;
import com.facturaarca.dto.FacturaVentaRequestDTO;
import com.facturaarca.entities.*;
import com.facturaarca.enums.EstadoFactura;
import com.facturaarca.repository.*;
import com.facturaarca.wsfe.Err;
import com.facturaarca.wsfe.FECAEDetResponse;
import com.facturaarca.wsfe.FECAEResponse;
import com.facturaarca.wsfe.Obs;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Servicio encargado de coordinar la emisión electrónica de comprobantes con ARCA/AFIP (WSFEv1)
 * y la persistencia de los datos fiscales (CAE, vencimiento, número correlativo) en PostgreSQL.
 */
@Service
@RequiredArgsConstructor
public class FacturacionService {

    private static final Logger log = LoggerFactory.getLogger(FacturacionService.class);

    private final FacturaVentaRepository facturaVentaRepository;
    private final PuntoVentaRepository puntoVentaRepository;
    private final CondicionIvaRepository condicionIvaRepository;
    private final TipoMonedaRepository tipoMonedaRepository;
    private final ClienteRepository clienteRepository;
    private final ListaPrecioArticuloRepository listaPrecioArticuloRepository;
    private final UsuarioRepository usuarioRepository;
    private final FacturaService facturaService;

    @Value("${afip.cuit:20000000001}") //Dato a cambiar
    private Long afipCuit;

    @Value("${afip.key.path:}")
    private String keyPathConfig;

    @Value("${afip.cert.path:}")
    private String certPathConfig;

    /**
     * Emite una nueva factura solicitando el CAE en ARCA/AFIP y persistiendo la factura autorizada.
     *
     * @param dto Datos de la factura y renglones enviados desde el cliente
     * @return FacturaVentaDTO con los datos completos, CAE asignado y número fiscal
     */
    @Transactional(noRollbackFor = {ResponseStatusException.class})
    public FacturaVentaDTO emitirFactura(FacturaVentaRequestDTO dto) {
        validarDatosObligatorios(dto);

        PuntoVenta puntoVenta = resolverPuntoVenta(dto.getPuntoVentaId());
        CondicionIva condicionIva = resolverCondicionIva(dto.getCondicionIvaId());
        TipoMoneda tipoMoneda = resolverTipoMoneda(dto.getTipoMonedaId());
        Cliente cliente = resolverCliente(dto.getClienteId());
        Usuario usuario = obtenerUsuarioParaAuditoria();

        // 1. Instanciar la entidad FacturaVenta en memoria y armar detalles
        FacturaVenta factura = new FacturaVenta();
        factura.setPuntoVenta(puntoVenta);
        factura.setCondicionIva(condicionIva);
        factura.setTipoMoneda(tipoMoneda);
        factura.setCliente(cliente);
        factura.setEstado(EstadoFactura.EN_PROCESO);
        factura.setObservaciones(dto.getObservaciones());
        factura.setFechaAlta(new Date());
        factura.setFechaModificacion(new Date());
        if (usuario != null) {
            factura.setUsuarioCarga(usuario);
            factura.setUsuarioModificacion(usuario);
        }

        agregarDetalles(factura, dto.getDetalles());
        factura.recalcularTotales();

        // 2. Gestionar la autorización con ARCA / AFIP
        return procesarAutorizacionAfip(factura, puntoVenta, usuario);
    }

    /**
     * Emite una factura previamente guardada en estado BORRADOR o RECHAZADA.
     */
    @Transactional(noRollbackFor = {ResponseStatusException.class})
    public FacturaVentaDTO emitirFacturaExistente(Long id) {
        FacturaVenta factura = facturaVentaRepository.buscarConDetalles(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada con id: " + id));

        if (!factura.getEstado().esInformable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede emitir una factura en estado " + factura.getEstado() + ". Debe estar en BORRADOR o RECHAZADA.");
        }

        Usuario usuario = obtenerUsuarioParaAuditoria();
        factura.marcarEnProceso(usuario);

        return procesarAutorizacionAfip(factura, factura.getPuntoVenta(), usuario);
    }

    /**
     * Núcleo del flujo con ARCA / AFIP:
     * 1. Obtiene Token y Sign de WSAA
     * 2. Consulta último comprobante en WSFEv1
     * 3. Solicita CAE
     * 4. Persiste el resultado en PostgreSQL
     */
    private FacturaVentaDTO procesarAutorizacionAfip(FacturaVenta factura, PuntoVenta puntoVenta, Usuario usuario) {
        int ptoVtaNumero = (puntoVenta != null && puntoVenta.getNumero() > 0) ? puntoVenta.getNumero() : 1;
        int cbteTipo = 11; // 11 = Factura C

        // Resolución dinámica idéntica a AfipIntegrationTest.java
        String rutaKey = resolverRuta(keyPathConfig, "backend/afip_private.key", "afip_private.key");
        String rutaCert = resolverRuta(certPathConfig, "backend/afip_certificado.crt", "afip_certificado.crt");

        try {
            log.info("Rutas credenciales resueltas -> Key: '{}', Cert: '{}'", rutaKey, rutaCert);
            log.info("1. Obteniendo Token y Sign desde WSAA para CUIT: {}", afipCuit);
            String[] auth = AfipWsaaClient.obtenerTokenYSign(rutaKey, rutaCert, "wsfe");
            String token = auth[0];
            String sign = auth[1];

            log.info("2. Consultando último comprobante autorizado en WSFEv1 (PtoVta: {}, CbteTipo: {})", ptoVtaNumero, cbteTipo);
            int ultimoNro = AfipWsfeClient.obtenerUltimoComprobante(token, sign, afipCuit, ptoVtaNumero, cbteTipo);
            long nuevoNro = ultimoNro + 1;
            log.info("   Último comprobante registrado: {} -> Próximo número a emitir: {}", ultimoNro, nuevoNro);

            log.info("3. Solicita CAE a WSFEv1 por importe total: ${}", factura.getImporteTotal());
            FECAEResponse resp = AfipWsfeClient.solicitarCaeFacturaC(
                    token, sign, afipCuit, ptoVtaNumero, nuevoNro, factura.getImporteTotal());

            // 4. Evaluar respuesta de ARCA / AFIP
            if (resp.getFeCabResp() != null && "A".equals(resp.getFeCabResp().getResultado())) {
                FECAEDetResponse detResp = resp.getFeDetResp().getFECAEDetResponse().get(0);
                String cae = detResp.getCAE();
                String caeFchVtoStr = detResp.getCAEFchVto();
                Date vtoDate = parsearFechaAfip(caeFchVtoStr);

                log.info("¡Comprobante Nro {} autorizado con éxito! CAE: {}, Vto: {}", nuevoNro, cae, caeFchVtoStr);

                factura.autorizar(nuevoNro, new Date(), cae, vtoDate, usuario);
                FacturaVenta guardada = facturaVentaRepository.saveAndFlush(factura);

                // Recargar con EntityGraph para asegurar que las colecciones y entidades asociadas estén inicializadas
                FacturaVenta facturaCompleta = facturaVentaRepository.buscarConDetalles(guardada.getId())
                        .orElse(guardada);

                return facturaService.convertirAFacturaVentaDTO(facturaCompleta);
            } else {
                String motivoRechazo = extraerMotivoRechazo(resp);
                log.error("Factura rechazada por ARCA: {}", motivoRechazo);

                factura.rechazar(motivoRechazo, usuario);
                facturaVentaRepository.saveAndFlush(factura);

                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "ARCA/AFIP rechazó la emisión del comprobante: " + motivoRechazo);
            }

        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (UnknownHostException | ConnectException | SocketTimeoutException ex) {
            String mensaje = "El servicio de homologación de ARCA/AFIP se encuentra temporalmente inaccesible o no hay conexión de red.";
            log.error("{}: {}", mensaje, ex.getMessage());
            factura.rechazar(mensaje, usuario);
            try {
                facturaVentaRepository.saveAndFlush(factura);
            } catch (Exception ignored) {}

            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, mensaje, ex);
        } catch (Exception ex) {
            String mensaje = "Error al comunicarse con los servicios de ARCA/AFIP: " + ex.getMessage();
            log.error(mensaje, ex);
            factura.setEstado(EstadoFactura.RECHAZADA);
            factura.setMotivoRechazo(mensaje);
            factura.setResultadoAfip("R");
            factura.setFechaModificacion(new Date());
            if (usuario != null) {
                factura.setUsuarioModificacion(usuario);
            }
            try {
                facturaVentaRepository.saveAndFlush(factura);
            } catch (Exception ignored) {}

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, mensaje, ex);
        }
    }

    private Date parsearFechaAfip(String fechaStr) {
        if (fechaStr == null || fechaStr.isBlank()) {
            return null;
        }
        try {
            return new SimpleDateFormat("yyyyMMdd").parse(fechaStr);
        } catch (Exception e) {
            log.warn("No se pudo parsear fecha de vencimiento CAE '{}': {}", fechaStr, e.getMessage());
            return null;
        }
    }

    private String extraerMotivoRechazo(FECAEResponse resp) {
        StringBuilder sb = new StringBuilder();
        if (resp.getErrors() != null && !resp.getErrors().getErr().isEmpty()) {
            for (Err err : resp.getErrors().getErr()) {
                sb.append("[").append(err.getCode()).append("] ").append(err.getMsg()).append(" ");
            }
        }
        if (resp.getFeDetResp() != null && !resp.getFeDetResp().getFECAEDetResponse().isEmpty()) {
            FECAEDetResponse det = resp.getFeDetResp().getFECAEDetResponse().get(0);
            if (det.getObservaciones() != null && !det.getObservaciones().getObs().isEmpty()) {
                for (Obs obs : det.getObservaciones().getObs()) {
                    sb.append("Obs [").append(obs.getCode()).append("] ").append(obs.getMsg()).append(" ");
                }
            }
        }
        return sb.length() > 0 ? sb.toString().trim() : "Rechazo sin detalle especificado por AFIP.";
    }

    private String resolverRuta(String configurada, String opcion1, String opcion2) {
        if (configurada != null && !configurada.isBlank() && new File(configurada).exists()) {
            return configurada;
        }
        if (new File(opcion1).exists()) {
            return opcion1;
        }
        if (new File(opcion2).exists()) {
            return opcion2;
        }
        return opcion1;
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los datos de la factura no pueden ser nulos");
        }
        if (dto.getPuntoVentaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El punto de venta es obligatorio");
        }
        if (dto.getCondicionIvaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La condición IVA es obligatoria");
        }
        if (dto.getTipoMonedaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de moneda es obligatorio");
        }
        if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La factura debe tener al menos un detalle");
        }
    }

    private PuntoVenta resolverPuntoVenta(Long id) {
        return puntoVentaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El punto de venta no existe (id: " + id + ")"));
    }

    private CondicionIva resolverCondicionIva(Long id) {
        return condicionIvaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La condición IVA no existe (id: " + id + ")"));
    }

    private TipoMoneda resolverTipoMoneda(Long id) {
        return tipoMonedaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de moneda no existe (id: " + id + ")"));
    }

    private Cliente resolverCliente(Long id) {
        if (id == null) {
            return null;
        }
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cliente no existe (id: " + id + ")"));
    }

    private ListaPrecioArticulo resolverListaPrecioArticulo(FacturaVentaDetalleRequestDTO detalleDto) {
        if (detalleDto.getListaPrecioArticuloId() != null) {
            ListaPrecioArticulo lista = listaPrecioArticuloRepository
                    .findById(detalleDto.getListaPrecioArticuloId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La lista de precios del artículo no existe (id: " + detalleDto.getListaPrecioArticuloId() + ")"));

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
                            "El artículo no tiene una lista de precios asociada (id: " + detalleDto.getArticuloId() + ")"));
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Debe especificar listaPrecioArticuloId o articuloId para cada detalle");
    }

    private Usuario obtenerUsuarioParaAuditoria() {
        return usuarioRepository.findAll().stream().findFirst().orElse(null);
    }
}
