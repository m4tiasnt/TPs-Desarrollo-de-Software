package com.facturaarca;

import com.facturaarca.services.ReporteService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
public class FacturaArcaApplication implements CommandLineRunner {

    private final ReporteService reporteService;

    public FacturaArcaApplication(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    public static void main(String[] args) {
        SpringApplication.run(FacturaArcaApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        reporteService.generarReportePdf();
        reporteService.generarReporteExcel();
    }
}
