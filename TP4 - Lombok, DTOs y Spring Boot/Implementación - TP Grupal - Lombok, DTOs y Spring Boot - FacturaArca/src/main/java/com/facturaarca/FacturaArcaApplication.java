package com.facturaarca;

import com.facturaarca.services.FacturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
public class FacturaArcaApplication implements CommandLineRunner {

    private final FacturaService facturaService;

    public FacturaArcaApplication(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    public static void main(String[] args) {
        SpringApplication.run(FacturaArcaApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        facturaService.generarReporteFacturasPdf();
    }
}
