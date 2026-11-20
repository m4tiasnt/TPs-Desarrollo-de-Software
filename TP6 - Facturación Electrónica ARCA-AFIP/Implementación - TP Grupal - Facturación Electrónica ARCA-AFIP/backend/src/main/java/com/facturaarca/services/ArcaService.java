package com.facturaarca.services;

import com.facturaarca.arca.ArcaWsaaClient;
import com.facturaarca.arca.ArcaWsfeClient;
import com.facturaarca.arca.ResultadoAutorizacionArca;
import com.facturaarca.entities.Cliente;
import com.facturaarca.entities.FacturaVenta;
import com.facturaarca.wsfe.Err;
import com.facturaarca.wsfe.FECAEDetResponse;
import com.facturaarca.wsfe.FECAEResponse;
import com.facturaarca.wsfe.Obs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de infraestructura e integración con ARCA / AFIP (WSAA + WSFEv1).
 * Emite comprobantes tipo Factura C (código AFIP 11) en Pesos Argentinos.
 */
@Service
public class ArcaService {

    private static final Logger log = LoggerFactory.getLogger(ArcaService.class);
    private static final int CBTE_TIPO_FACTURA_C = 11;
    private static final DateTimeFormatter FORMATO_FECHA_ARCA = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ConcurrentHashMap<Integer, Object> locksPorPuntoVenta = new ConcurrentHashMap<>();

    @Value("${arca.cuit}")
    private Long arcaCuit;

    @Value("${arca.key.path:${afip.key.path:}}")
    private String keyPathConfig;

    @Value("${arca.cert.path:${afip.cert.path:}}")
    private String certPathConfig;

    /**
     * Autenticación preventiva contra WSAA al arrancar la aplicación para que la primera
     * factura emitida no sufra demoras ni bloquee el hilo principal.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void precargarTokenWsaa() {
        CompletableFuture.runAsync(() -> {
            String rutaKey = resolverRuta(keyPathConfig, "backend/afip_private.key", "afip_private.key");
            String rutaCert = resolverRuta(certPathConfig, "backend/afip_certificado.crt", "afip_certificado.crt");
            if (new File(rutaKey).exists() && new File(rutaCert).exists()) {
                log.info("ARCA: Autenticando preventivamente contra WSAA al iniciar la aplicación...");
                try {
                    ArcaWsaaClient.obtenerTokenYSign(rutaKey, rutaCert, "wsfe");
                    log.info("ARCA: Autenticación inicial con WSAA exitosa. Token y Sign cacheados para 'wsfe'.");
                } catch (Exception e) {
                    log.warn("ARCA: Aviso al iniciar en conexión WSAA: {}", e.getMessage());
                }
            } else {
                log.info("ARCA: No se encontraron certificados locales ({}, {}). Se omitió la autenticación inicial.", rutaKey, rutaCert);
            }
        });
    }

    /**
     * Solicita la autorización fiscal de una Factura C ante ARCA.
     * Sincroniza por punto de venta para evitar colisiones de numeración correlativa.
     */
    public ResultadoAutorizacionArca solicitarAutorizacion(FacturaVenta factura) {
        String rutaKey = resolverRuta(keyPathConfig, "backend/afip_private.key", "afip_private.key");
        String rutaCert = resolverRuta(certPathConfig, "backend/afip_certificado.crt", "afip_certificado.crt");

        if (!new File(rutaKey).exists() || !new File(rutaCert).exists()) {
            String msg = "No se encontraron las credenciales de ARCA (clave privada / certificado) en el servidor. Verificá la configuración.";
            log.error("{} [Key: '{}', Cert: '{}']", msg, rutaKey, rutaCert);
            return ResultadoAutorizacionArca.builder()
                    .autorizado(false)
                    .motivoRechazo(msg)
                    .mensaje(msg)
                    .build();
        }

        int ptoVta = (factura.getPuntoVenta() != null && factura.getPuntoVenta().getNumero() > 0)
                ? factura.getPuntoVenta().getNumero() : 1;

        int docTipo = 99; // 99 = Consumidor Final
        long docNro = 0L;
        int condicionIvaReceptor = 5; // 5 = Consumidor Final

        Cliente cliente = factura.getCliente();
        if (cliente != null && cliente.getCuitCuil() != null && !cliente.getCuitCuil().isBlank()) {
            String docLimpio = cliente.getCuitCuil().replaceAll("\\D", "");
            if (!docLimpio.isEmpty() && docLimpio.length() <= 18) {
                docNro = Long.parseLong(docLimpio);
                docTipo = docLimpio.length() == 11 ? 80 : 96;
                if (factura.getCondicionIva() != null && factura.getCondicionIva().getCodigoAfip() > 0) {
                    condicionIvaReceptor = factura.getCondicionIva().getCodigoAfip();
                }
            }
        }

        double total = Math.round(factura.getImporteTotal() * 100.0) / 100.0;
        // Factura C no discrimina IVA ante AFIP: ImpNeto = ImpTotal e ImpIVA = 0
        double neto = total;
        double iva = 0.00;

        Object lock = locksPorPuntoVenta.computeIfAbsent(ptoVta, k -> new Object());
        synchronized (lock) {
            return ejecutarAutorizacionConReintentoToken(
                    rutaKey, rutaCert, ptoVta, docTipo, docNro, condicionIvaReceptor, total, neto, iva);
        }
    }

    private ResultadoAutorizacionArca ejecutarAutorizacionConReintentoToken(
            String rutaKey, String rutaCert, int ptoVta,
            int docTipo, long docNro, int condicionIvaReceptor,
            double total, double neto, double iva) {
        for (int intento = 0; intento < 2; intento++) {
            try {
                log.info("1. Obteniendo Token y Sign desde WSAA para CUIT: {}", arcaCuit);
                String[] auth = ArcaWsaaClient.obtenerTokenYSign(rutaKey, rutaCert, "wsfe");
                String token = auth[0];
                String sign = auth[1];

                log.info("2. Consultando último comprobante autorizado en WSFEv1 (PtoVta: {}, CbteTipo: {})",
                        ptoVta, CBTE_TIPO_FACTURA_C);
                int ultimoNro = ArcaWsfeClient.obtenerUltimoComprobante(
                        token, sign, arcaCuit, ptoVta, CBTE_TIPO_FACTURA_C);
                long nuevoNro = ultimoNro + 1;
                log.info("   Último comprobante registrado: {} -> Próximo número a emitir: {}", ultimoNro, nuevoNro);

                log.info("3. Solicitando CAE a WSFEv1 por importe total: ${}", total);
                FECAEResponse resp = ArcaWsfeClient.solicitarCae(
                        token, sign, arcaCuit, ptoVta, CBTE_TIPO_FACTURA_C, nuevoNro,
                        docTipo, docNro, condicionIvaReceptor, total, neto, iva);

                if (resp != null && resp.getFeCabResp() != null && "A".equals(resp.getFeCabResp().getResultado())
                        && resp.getFeDetResp() != null && !resp.getFeDetResp().getFECAEDetResponse().isEmpty()) {
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
                }

                if (intento == 0 && esErrorTokenInvalido(resp)) {
                    log.warn("Token WSAA rechazado por WSFEv1; invalidando caché y reintentando...");
                    ArcaWsaaClient.invalidarCache();
                    continue;
                }

                String motivoRechazo = extraerMotivoRechazo(resp);
                log.error("Factura rechazada por ARCA: {}", motivoRechazo);
                return ResultadoAutorizacionArca.builder()
                        .autorizado(false)
                        .motivoRechazo(motivoRechazo)
                        .mensaje(motivoRechazo)
                        .build();

            } catch (IllegalStateException ex) {
                if (intento == 0 && contieneCodigoErrorToken(ex.getMessage())) {
                    log.warn("Token WSAA rechazado en consulta previa ({}); invalidando caché y reintentando...", ex.getMessage());
                    ArcaWsaaClient.invalidarCache();
                    continue;
                }
                log.error("Error controlado al comunicar con ARCA: {}", ex.getMessage());
                return ResultadoAutorizacionArca.builder()
                        .autorizado(false)
                        .motivoRechazo(ex.getMessage())
                        .mensaje(ex.getMessage())
                        .build();
            } catch (Exception ex) {
                if (esErrorDeRed(ex)) {
                    String mensaje = "El servicio de ARCA/AFIP se encuentra temporalmente inaccesible o no hay conexión de red.";
                    log.error("{}: {}", mensaje, ex.getMessage());
                    return ResultadoAutorizacionArca.builder()
                            .autorizado(false)
                            .motivoRechazo(mensaje)
                            .mensaje(mensaje)
                            .build();
                }
                log.error("Error de comunicación con ARCA", ex);
                String mensaje = "No se pudo completar la comunicación con ARCA. Intentá nuevamente en unos instantes.";
                return ResultadoAutorizacionArca.builder()
                        .autorizado(false)
                        .motivoRechazo(mensaje)
                        .mensaje(mensaje)
                        .build();
            }
        }
        String fallback = "No se pudo completar la autenticación con ARCA.";
        return ResultadoAutorizacionArca.builder()
                .autorizado(false)
                .motivoRechazo(fallback)
                .mensaje(fallback)
                .build();
    }

    private static boolean esErrorTokenInvalido(FECAEResponse resp) {
        if (resp == null || resp.getErrors() == null) {
            return false;
        }
        return resp.getErrors().getErr().stream()
                .anyMatch(err -> err.getCode() == 600 || err.getCode() == 601 || err.getCode() == 602);
    }

    private static boolean contieneCodigoErrorToken(String mensaje) {
        if (mensaje == null) {
            return false;
        }
        return mensaje.contains("[600]") || mensaje.contains("[601]") || mensaje.contains("[602]");
    }

    private static boolean esErrorDeRed(Throwable ex) {
        Throwable actual = ex;
        while (actual != null) {
            if (actual instanceof UnknownHostException
                    || actual instanceof ConnectException
                    || actual instanceof SocketTimeoutException) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }

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
        if (resp == null) {
            return "ARCA no devolvió respuesta para la solicitud.";
        }
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
}
