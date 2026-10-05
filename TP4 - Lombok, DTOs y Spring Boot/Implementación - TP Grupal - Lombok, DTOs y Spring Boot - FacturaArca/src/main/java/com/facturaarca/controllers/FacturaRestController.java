package com.facturaarca.controllers;

import com.facturaarca.dto.FacturaReporteDTO;
import com.facturaarca.services.FacturaService;
import com.facturaarca.services.ReporteTxtService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/facturas")
public class FacturaRestController {

    private static final String BOM_UTF8 = "\uFEFF"; 

    private final FacturaService facturaService;
    private final ReporteTxtService reporteTxtService;

    public FacturaRestController(FacturaService facturaService, ReporteTxtService reporteTxtService) {
        this.facturaService = facturaService;
        this.reporteTxtService = reporteTxtService;
    }

    @GetMapping
    public List<FacturaReporteDTO> getFacturas(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double montoMinimo) {

        return facturaService.buscarFacturasFiltradas(fechaDesde, fechaHasta, estado, montoMinimo);
    }
    // ENDPOINT (TXT) 
    @GetMapping("/txt")
    public ResponseEntity<byte[]> descargarTxt(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double montoMinimo) {

        List<FacturaReporteDTO> facturas =
                facturaService.buscarFacturasFiltradas(fechaDesde, fechaHasta, estado, montoMinimo);

        byte[] contenido = (BOM_UTF8 + reporteTxtService.construirContenido(facturas))
                .getBytes(StandardCharsets.UTF_8);

        String nombre = "reporte_facturas_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + ".txt";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(contenido);
    }
}