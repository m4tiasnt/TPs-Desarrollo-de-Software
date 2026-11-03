package com.facturaarca;

import com.facturaarca.wsfe.FECAEDetResponse;
import com.facturaarca.wsfe.FECAEResponse;
import com.facturaarca.wsfe.Err;
import com.facturaarca.wsfe.Obs;

import java.io.File;

public class AfipIntegrationTest {

    public static void main(String[] args) {
        try {
            // Resolver rutas tanto si se ejecuta desde la raíz del proyecto como desde la carpeta 'backend'
            String keyPath = new File("backend/afip_private.key").exists() ? "backend/afip_private.key" : "afip_private.key";
            String certPath = new File("backend/afip_certificado.crt").exists() ? "backend/afip_certificado.crt" : "afip_certificado.crt";

            // CUIT obtenido del certificado digital (homologación)
            long cuit = 20000000001L;
            int puntoDeVenta = 1;
            double importeFactura = 12100.00;

            System.out.println("==================================================");
            System.out.println("1. Solicitando Token y Sign a WSAA (Homologación)...");
            System.out.println("==================================================");
            String[] auth = AfipWsaaClient.obtenerTokenYSign(keyPath, certPath, "wsfe");

            String token = auth[0];
            String sign = auth[1];

            System.out.println("Autenticación exitosa con WSAA.");
            System.out.println("TOKEN: " + token.substring(0, Math.min(30, token.length())) + "...");
            System.out.println("SIGN:  " + sign.substring(0, Math.min(30, sign.length())) + "...");

            System.out.println("\n==================================================");
            System.out.println("2. Emisión de Factura C mediante WSFEv1...");
            System.out.println("==================================================");

            FECAEResponse resp = AfipWsfeClient.emitirFacturaCConsumidorFinal(token, sign, cuit, puntoDeVenta, importeFactura);

            // Validar resultado global y extraer CAE
            if (resp.getFeCabResp() != null && "A".equals(resp.getFeCabResp().getResultado())) {
                FECAEDetResponse det = resp.getFeDetResp().getFECAEDetResponse().get(0);
                String cae = det.getCAE();
                String vtoCae = det.getCAEFchVto();
                long cbteNro = det.getCbteDesde();

                System.out.println("\n**************************************************");
                System.out.println("¡Factura autorizada con éxito por ARCA / AFIP!");
                System.out.println("Comprobante Nro: " + cbteNro);
                System.out.println("Punto de Venta:  " + puntoDeVenta);
                System.out.println("CAE:             " + cae);
                System.out.println("Vencimiento CAE: " + vtoCae);
                System.out.println("**************************************************");

                if (det.getObservaciones() != null && !det.getObservaciones().getObs().isEmpty()) {
                    System.out.println("\nObservaciones AFIP:");
                    for (Obs obs : det.getObservaciones().getObs()) {
                        System.out.println(" - [" + obs.getCode() + "] " + obs.getMsg());
                    }
                }
            } else {
                System.err.println("\n[!] Solicitud rechazada por ARCA / AFIP:");
                if (resp.getFeCabResp() != null) {
                    System.err.println("Resultado cabecera: " + resp.getFeCabResp().getResultado());
                }
                if (resp.getErrors() != null && !resp.getErrors().getErr().isEmpty()) {
                    for (Err err : resp.getErrors().getErr()) {
                        System.err.println("Error general [" + err.getCode() + "]: " + err.getMsg());
                    }
                }
                if (resp.getFeDetResp() != null && !resp.getFeDetResp().getFECAEDetResponse().isEmpty()) {
                    FECAEDetResponse det = resp.getFeDetResp().getFECAEDetResponse().get(0);
                    if (det.getObservaciones() != null && !det.getObservaciones().getObs().isEmpty()) {
                        for (Obs obs : det.getObservaciones().getObs()) {
                            System.err.println("Obs detalle [" + obs.getCode() + "]: " + obs.getMsg());
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("\nError durante el proceso con ARCA / AFIP:");
            e.printStackTrace();
        }
    }
}
