package com.facturaarca;

import com.facturaarca.wsfe.*;
import jakarta.xml.ws.BindingProvider;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Cliente SOAP para el servicio de Facturación Electrónica WSFEv1 de ARCA / AFIP (Homologación).
 */
public class AfipWsfeClient {

    private static final String URL_WSFE_HOMO = "https://wswhomo.afip.gov.ar/wsfev1/service.asmx";

    /**
     * Obtiene el puerto SOAP configurado con el endpoint de homologación y timeouts.
     */
    private static ServiceSoap obtenerPuertoSoap() {
        Service service = new Service();
        ServiceSoap serviceSoap = service.getServiceSoap();

        BindingProvider bp = (BindingProvider) serviceSoap;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, URL_WSFE_HOMO);
        bp.getRequestContext().put("com.sun.xml.ws.connect.timeout", 15000);
        bp.getRequestContext().put("com.sun.xml.ws.request.timeout", 30000);

        return serviceSoap;
    }

    /**
     * Consulta el último número de comprobante autorizado para un punto de venta y tipo de comprobante.
     *
     * @param token      Token de autenticación obtenido de WSAA
     * @param sign       Firma de autenticación obtenida de WSAA
     * @param cuit       CUIT del emisor
     * @param puntoVenta Punto de venta a consultar (ej. 1 o 2)
     * @param cbteTipo   Tipo de comprobante (ej. 11 = Factura C)
     * @return El último número de comprobante emitido (0 si no se emitió ninguno aún)
     */
    public static int obtenerUltimoComprobante(String token, String sign, long cuit, int puntoVenta, int cbteTipo) throws Exception {
        ServiceSoap serviceSoap = obtenerPuertoSoap();

        FEAuthRequest auth = new FEAuthRequest();
        auth.setToken(token);
        auth.setSign(sign);
        auth.setCuit(cuit);

        FERecuperaLastCbteResponse resp = serviceSoap.feCompUltimoAutorizado(auth, puntoVenta, cbteTipo);

        if (resp != null && resp.getErrors() != null && !resp.getErrors().getErr().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Err err : resp.getErrors().getErr()) {
                sb.append("[").append(err.getCode()).append("] ").append(err.getMsg()).append(" ");
            }
            throw new RuntimeException("Error en FECompUltimoAutorizado: " + sb.toString());
        }

        return resp != null ? resp.getCbteNro() : 0;
    }

    /**
     * Arma y solicita el CAE para una Factura C (cbteTipo = 11) a consumidor final (docTipo = 99).
     *
     * @param token          Token obtenido de WSAA
     * @param sign           Sign obtenido de WSAA
     * @param cuit           CUIT del emisor
     * @param puntoVenta     Punto de venta
     * @param nroComprobante Número de comprobante a autorizar (generalmente último + 1)
     * @param importeTotal   Importe total del comprobante
     * @return Respuesta completa del WSFEv1 con el resultado y CAE
     */
    public static FECAEResponse solicitarCaeFacturaC(String token, String sign, long cuit, int puntoVenta, long nroComprobante, double importeTotal) throws Exception {
        ServiceSoap serviceSoap = obtenerPuertoSoap();

        // 1. Configurar credenciales de autenticación
        FEAuthRequest auth = new FEAuthRequest();
        auth.setToken(token);
        auth.setSign(sign);
        auth.setCuit(cuit);

        int cbteTipo = 11; // Factura C

        // 2. Armar la solicitud de factura (FECAESolicitar)
        FECAERequest feReq = new FECAERequest();

        // Cabecera del lote
        FECAECabRequest cab = new FECAECabRequest();
        cab.setCantReg(1);
        cab.setPtoVta(puntoVenta);
        cab.setCbteTipo(cbteTipo);
        feReq.setFeCabReq(cab);

        // Detalle del comprobante
        ArrayOfFECAEDetRequest detArray = new ArrayOfFECAEDetRequest();
        FECAEDetRequest det = new FECAEDetRequest();
        det.setConcepto(1); // 1 = Productos
        det.setDocTipo(99); // 99 = Consumidor Final
        det.setDocNro(0L);  // 0 para consumidor final
        det.setCondicionIVAReceptorId(5); // 5 = Consumidor Final (RG 5616)
        det.setCbteDesde(nroComprobante);
        det.setCbteHasta(nroComprobante);

        // Fecha en formato YYYYMMDD
        String fechaHoy = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        det.setCbteFch(fechaHoy);

        det.setImpTotal(importeTotal);
        det.setImpTotConc(0.00);
        det.setImpNeto(importeTotal);
        det.setImpOpEx(0.00);
        det.setImpTrib(0.00);
        det.setImpIVA(0.00); // Factura C no discrimina IVA
        det.setMonId("PES");
        det.setMonCotiz(1.0);

        detArray.getFECAEDetRequest().add(det);
        feReq.setFeDetReq(detArray);

        // 3. Invocar el método de autorización SOAP
        return serviceSoap.fecaeSolicitar(auth, feReq);
    }

    /**
     * Flujo completo integrado:
     * 1. Consulta el último comprobante autorizado (FECompUltimoAutorizado) para calcular el siguiente número.
     * 2. Arma la solicitud y pide el CAE mediante FECAESolicitar para una Factura C a consumidor final.
     *
     * @param token      Token obtenido de WSAA
     * @param sign       Sign obtenido de WSAA
     * @param cuit       CUIT del emisor
     * @param puntoVenta Punto de venta
     * @param importe    Importe total de la factura
     * @return Respuesta de autorización FECAEResponse
     */
    public static FECAEResponse emitirFacturaCConsumidorFinal(String token, String sign, long cuit, int puntoVenta, double importe) throws Exception {
        int cbteTipo = 11; // Factura C

        System.out.println("2. Consultando último comprobante autorizado (PtoVta=" + puntoVenta + ", CbteTipo=" + cbteTipo + ")...");
        int ultimoNro = obtenerUltimoComprobante(token, sign, cuit, puntoVenta, cbteTipo);
        long nuevoNro = ultimoNro + 1;
        System.out.println("   Último número registrado: " + ultimoNro + ". Siguiente a emitir: " + nuevoNro);

        System.out.println("3. Enviando factura a WSFEv1 para solicitud de CAE...");
        return solicitarCaeFacturaC(token, sign, cuit, puntoVenta, nuevoNro, importe);
    }

    /**
     * Sobrecarga que recibe token, sign y cuit con punto de venta 1 y monto por defecto.
     */
    public static FECAEResponse emitirFacturaCConsumidorFinal(String token, String sign, long cuit) throws Exception {
        return emitirFacturaCConsumidorFinal(token, sign, cuit, 1, 12100.00);
    }
}
