package com.facturaarca.controllers;

import com.facturaarca.enums.EstadoFactura;
import com.facturaarca.services.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reportes")
public class ReporteRestController {

    private final ReporteService reporteService;

    public ReporteRestController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    /**
     * Descarga el reporte consolidado de facturas en formato PDF respetando los filtros activos.
     */
    @GetMapping("/facturas/pdf")
    public ResponseEntity<byte[]> descargarReportePdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoFactura estado,
            @RequestParam(required = false) Double montoMinimo,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean informadaArca) throws IOException {
        byte[] pdfBytes = reporteService.generarReportePdfBytes(
                fechaDesde, fechaHasta, estado, montoMinimo, clienteId, busqueda, informadaArca);
        String filename = "reporte_facturas_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }

    /**
     * Descarga el reporte consolidado de facturas en formato Microsoft Excel (.xlsx) respetando los filtros activos.
     */
    @GetMapping("/facturas/excel")
    public ResponseEntity<byte[]> descargarReporteExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoFactura estado,
            @RequestParam(required = false) Double montoMinimo,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean informadaArca) throws IOException {
        byte[] excelBytes = reporteService.generarReporteExcelBytes(
                fechaDesde, fechaHasta, estado, montoMinimo, clienteId, busqueda, informadaArca);
        String filename = "reporte_facturas_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

        MediaType excelMediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(excelMediaType)
                .contentLength(excelBytes.length)
                .body(excelBytes);
    }
}
