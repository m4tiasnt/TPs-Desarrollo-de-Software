package com.facturaarca.arca;

import com.facturaarca.wsfe.*;
import jakarta.xml.ws.BindingProvider;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Cliente SOAP para el servicio de Facturación Electrónica WSFEv1 de ARCA / AFIP (Homologación).
 */
public class ArcaWsfeClient {

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
     * Solicita CAE ante WSFEv1 de forma parametrizada.
     */
    public static FECAEResponse solicitarCae(String token, String sign, long cuit,
                                            int puntoVenta, int cbteTipo, long nroComprobante,
                                            int docTipo, long docNro, int condicionIvaReceptorId,
                                            double importeTotal, double importeNeto, double importeIva) throws Exception {
        ServiceSoap serviceSoap = obtenerPuertoSoap();

        // 1. Configurar credenciales de autenticación
        FEAuthRequest auth = new FEAuthRequest();
        auth.setToken(token);
        auth.setSign(sign);
        auth.setCuit(cuit);

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
        det.setDocTipo(docTipo);
        det.setDocNro(docNro);
        det.setCondicionIVAReceptorId(condicionIvaReceptorId);
        det.setCbteDesde(nroComprobante);
        det.setCbteHasta(nroComprobante);

        // Fecha en formato YYYYMMDD
        String fechaHoy = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        det.setCbteFch(fechaHoy);

        det.setImpTotal(importeTotal);
        det.setImpTotConc(0.00);
        det.setImpNeto(importeNeto);
        det.setImpOpEx(0.00);
        det.setImpTrib(0.00);
        det.setImpIVA(importeIva);
        det.setMonId("PES");
        det.setMonCotiz(1.0);

        // Si discrimina IVA (por ejemplo Factura A o B con importe IVA > 0), agregamos el array de alícuotas
        if (importeIva > 0 && cbteTipo != 11) {
            ArrayOfAlicIva ivaArray = new ArrayOfAlicIva();
            AlicIva alic = new AlicIva();
            alic.setId(5); // 5 = 21%
            alic.setBaseImp(importeNeto);
            alic.setImporte(importeIva);
            ivaArray.getAlicIva().add(alic);
            det.setIva(ivaArray);
        }

        detArray.getFECAEDetRequest().add(det);
        feReq.setFeDetReq(detArray);

        // 3. Invocar el método de autorización SOAP
        return serviceSoap.fecaeSolicitar(auth, feReq);
    }
}
