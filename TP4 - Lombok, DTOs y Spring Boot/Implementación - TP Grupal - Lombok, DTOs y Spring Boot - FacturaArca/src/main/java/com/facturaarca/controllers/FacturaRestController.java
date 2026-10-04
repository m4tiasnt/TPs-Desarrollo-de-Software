package com.facturaarca.controllers;

import com.facturaarca.dto.FacturaReporteDTO;
import com.facturaarca.services.FacturaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/facturas")
public class FacturaRestController {

    private final FacturaService facturaService;


    public FacturaRestController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @GetMapping
    public List<FacturaReporteDTO> getFacturas(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double montoMinimo) {

        // Llamamos al servicio creado por el Integrante 2
        return facturaService.buscarFacturasFiltradas(fechaDesde, fechaHasta, estado, montoMinimo);
    }
}