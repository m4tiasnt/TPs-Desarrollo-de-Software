package com.facturaarca.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ReporteTxtAutoGenerador {

    private final ReporteTxtService reporteTxtService;

    @Value("${server.port:8080}")
    private String puerto;

    public ReporteTxtAutoGenerador(ReporteTxtService reporteTxtService) {
        this.reporteTxtService = reporteTxtService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        try {
            reporteTxtService.generarReporteFacturasTxt();
        } catch (Exception e) {
            System.err.println("No se pudo generar el reporte TXT al arrancar: " + e.getMessage());
        }

        String base = "http://localhost:" + puerto + "/api/facturas";
        System.out.println();
        System.out.println("==============================================================");
        System.out.println(" Descargar reporte TXT : " + base + "/txt");
        System.out.println(" Ver facturas en JSON  : " + base);
        System.out.println("==============================================================");
    }
}