package com.facturaarca.services;

import com.facturaarca.arca.ArcaWsaaClient;
import com.facturaarca.arca.ArcaWsfeClient;
import com.facturaarca.arca.ResultadoAutorizacionArca;
import com.facturaarca.entities.Cliente;
import com.facturaarca.entities.FacturaVenta;
import com.facturaarca.entities.FacturaVentaDetalle;
import com.facturaarca.wsfe.Err;
import com.facturaarca.wsfe.FECAEDetResponse;
import com.facturaarca.wsfe.FECAEResponse;
import com.facturaarca.wsfe.Obs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import java.util.concurrent.CompletableFuture;

import java.io.File;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Servicio de infraestructura e integración con ARCA / AFIP (WSAA + WSFEv1).
 * Es un adaptador técnico sin dependencias con repositorios JPA de PostgreSQL.
 */
@Service
public class ArcaService {

    private static final Logger log = LoggerFactory.getLogger(ArcaService.class);

    @Value("${arca.cuit}")
    private Long arcaCuit;

    @Value("${arca.key.path:${afip.key.path:}}")
    private String keyPathConfig;

    @Value("${arca.cert.path:${afip.cert.path:}}")
    private String certPathConfig;

    /**
     * AutenticaciÃ³n preventiva contra WSAA al arrancar la aplicaciÃ³n para que la primera
     * factura emitida no sufra demoras ni bloquee el hilo principal.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void precargarTokenWsaa() {
        CompletableFuture.runAsync(() -> {
            String rutaKey = resolverRuta(keyPathConfig, "backend/afip_private.key", "afip_private.key");
            String rutaCert = resolverRuta(certPathConfig, "backend/afip_certificado.crt", "afip_certificado.crt");
            if (new File(rutaKey).exists() && new File(rutaCert).exists()) {
                log.info("ARCA: Autenticando preventivamente contra WSAA al iniciar la aplicaciÃ³n...");
                try {
                    String[] auth = ArcaWsaaClient.obtenerTokenYSign(rutaKey, rutaCert, "wsfe");
                    log.info("ARCA: AutenticaciÃ³n inicial con WSAA exitosa. Token y Sign cacheados para 'wsfe'.");
                } catch (Exception e) {
                    log.warn("ARCA: Aviso al iniciar en conexiÃ³n WSAA: {}", e.getMessage());
                }
            } else {
                log.info("ARCA: No se encontraron certificados locales ({}, {}). Se omitiÃ³ la autenticaciÃ³n inicial.", rutaKey, rutaCert);
            }
        });
    }

    /**
     * Solicita la autorizaciÃ³n fiscal de un comprobante ante ARCA.
     */
    public ResultadoAutorizacionArca solicitarAutorizacion(FacturaVenta factura) {
        String rutaKey = resolverRuta(keyPathConfig, "backend/afip_private.key", "afip_private.key");
        String rutaCert = resolverRuta(certPathConfig, "backend/afip_certificado.crt", "afip_certificado.crt");

        if (!new File(rutaKey).exists() || !new File(rutaCert).exists()) {
            String msg = "No se encontraron las credenciales de ARCA (clave privada / certificado) en el servidor. Verifique la configuraciÃ³n de arca.key.path y arca.cert.path.";
            log.error(msg + " [Key: '{}', Cert: '{}']", rutaKey, rutaCert);
            return ResultadoAutorizacionArca.builder()
                    .autorizado(false)
                    .motivoRechazo(msg)
                    .mensaje(msg)
                    .build();
        }

        int ptoVta = (factura.getPuntoVenta() != null && factura.getPuntoVenta().getNumero() > 0)
                ? factura.getPuntoVenta().getNumero() : 1;

        // Tipo de comprobante (por defecto 11 = Factura C de monotributo / homologaciÃ³n estÃ¡ndar)
        int cbteTipo = 11;

        // Receptor
        int docTipo = 99; // 99 = Consumidor Final
        long docNro = 0L;
        int condicionIvaReceptor = 5; // 5 = Consumidor Final

        Cliente cliente = factura.getCliente();
        if (cliente != null && cliente.getCuitCuil() != null && !cliente.getCuitCuil().isBlank()) {
            String docLimpio = cliente.getCuitCuil().replaceAll("\\D", "");
            if (!docLimpio.isEmpty() && esCuitValido(docLimpio)) {
                docNro = Long.parseLong(docLimpio);
                docTipo = docLimpio.length() == 11 ? 80 : 96;
                if (factura.getCondicionIva() != null && factura.getCondicionIva().getId() != null) {
                    long cIvaId = factura.getCondicionIva().getId();
                    if (cIvaId == 1) condicionIvaReceptor = 1;
                    else if (cIvaId == 6) condicionIvaReceptor = 6;
                }
            } else {
                // Si el CUIT no es matemÃ¡ticamente vÃ¡lido segÃºn algoritmo MÃ³dulo 11, emitir como Consumidor Final
                log.warn("El CUIT del cliente ('{}') no es vÃ¡lido segÃºn algoritmo MÃ³dulo 11. Se emite como Consumidor Final.", cliente.getCuitCuil());
                docTipo = 99;
                docNro = 0L;
                condicionIvaReceptor = 5;
            }
        }

        double total = factura.getImporteTotal();
        double neto = 0.0;
        double iva = 0.0;
        if (factura.getDetalles() != null) {
            neto = factura.getDetalles().stream().mapToDouble(FacturaVentaDetalle::getImporteNeto).sum();
            iva = factura.getDetalles().stream().mapToDouble(FacturaVentaDetalle::getImporteIva).sum();
        }
        neto = Math.round(neto * 100.0) / 100.0;
        iva = Math.round(iva * 100.0) / 100.0;

        if (cbteTipo == 11) {
            neto = total;
            iva = 0.00;
        }

        try {
            log.info("1. Obteniendo Token y Sign desde WSAA para CUIT: {}", arcaCuit);
            String[] auth = ArcaWsaaClient.obtenerTokenYSign(rutaKey, rutaCert, "wsfe");
            String token = auth[0];
            String sign = auth[1];

            log.info("2. Consultando Ãºltimo comprobante autorizado en WSFEv1 (PtoVta: {}, CbteTipo: {})", ptoVta, cbteTipo);
            int ultimoNro = ArcaWsfeClient.obtenerUltimoComprobante(token, sign, arcaCuit, ptoVta, cbteTipo);
            long nuevoNro = ultimoNro + 1;
            log.info("   Último comprobante registrado: {} -> Próximo número a emitir: {}", ultimoNro, nuevoNro);

            log.info("3. Solicitando CAE a WSFEv1 por importe total: ${}", total);
            FECAEResponse resp = ArcaWsfeClient.solicitarCae(
                    token, sign, arcaCuit, ptoVta, cbteTipo, nuevoNro,
                    docTipo, docNro, condicionIvaReceptor, total, neto, iva);

            if (resp.getFeCabResp() != null && "A".equals(resp.getFeCabResp().getResultado())) {
                FECAEDetResponse detResp = resp.getFeDetResp().getFECAEDetResponse().get(0);
                String cae = detResp.getCAE();
                Date vtoDate = parsearFechaAfip(detResp.getCAEFchVto());

                log.info("¡Comprobante Nro {} autorizado con éxito! CAE: {}", nuevoNro, cae);
                return ResultadoAutorizacionArca.builder()
                        .autorizado(true)
                        .numeroComprobante(nuevoNro)
                        .fechaEmision(new Date())
                        .cae(cae)
                        .caeVencimiento(vtoDate)
                        .mensaje("Autorizada por ARCA. CAE: " + cae)
                        .build();
            } else {
                String motivoRechazo = extraerMotivoRechazo(resp);
                log.error("Factura rechazada por ARCA: {}", motivoRechazo);
                return ResultadoAutorizacionArca.builder()
                        .autorizado(false)
                        .motivoRechazo(motivoRechazo)
                        .mensaje(motivoRechazo)
                        .build();
            }

        } catch (UnknownHostException | ConnectException | SocketTimeoutException ex) {
            String mensaje = "El servicio de ARCA/AFIP se encuentra temporalmente inaccesible o no hay conexiÃ³n de red.";
            log.error("{}: {}", mensaje, ex.getMessage());
            return ResultadoAutorizacionArca.builder()
                    .autorizado(false)
                    .motivoRechazo(mensaje)
                    .mensaje(mensaje)
                    .build();
        } catch (Exception ex) {
            String mensaje = "Error de comunicaciÃ³n con ARCA: " + ex.getMessage();
            log.error(mensaje, ex);
            return ResultadoAutorizacionArca.builder()
                    .autorizado(false)
                    .motivoRechazo(mensaje)
                    .mensaje(mensaje)
                    .build();
        }
    }

    private static final DateTimeFormatter FORMATO_FECHA_ARCA = DateTimeFormatter.ofPattern("yyyyMMdd");

    private Date parsearFechaAfip(String fechaStr) {
        if (fechaStr == null || fechaStr.isBlank()) {
            return null;
        }
        try {
            LocalDate localDate = LocalDate.parse(fechaStr.trim(), FORMATO_FECHA_ARCA);
            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
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
        return sb.length() > 0 ? sb.toString().trim() : "Rechazo sin detalle especificado por ARCA.";
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

    private static boolean esCuitValido(String cuit) {
        if (cuit == null || cuit.length() != 11) return false;
        int[] mult = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int total = 0;
        for (int i = 0; i < 10; i++) {
            total += Character.getNumericValue(cuit.charAt(i)) * mult[i];
        }
        int resto = total % 11;
        int digitoEsperado = (resto == 0) ? 0 : (resto == 1) ? 9 : 11 - resto;
        return digitoEsperado == Character.getNumericValue(cuit.charAt(10));
    }
}
