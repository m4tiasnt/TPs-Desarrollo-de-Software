package com.facturaarca.controllers;

import com.facturaarca.services.ReporteService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reportes")
@CrossOrigin(origins = "*")
public class ReporteRestController {

    private final ReporteService reporteService;

    public ReporteRestController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    /**
     * Descarga el reporte consolidado de facturas en formato PDF.
     */
    @GetMapping("/facturas/pdf")
    public ResponseEntity<byte[]> descargarReportePdf() throws IOException {
        byte[] pdfBytes = reporteService.generarReportePdfBytes();
        String filename = "reporte_facturas_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }

    /**
     * Descarga el reporte consolidado de facturas en formato Microsoft Excel (.xlsx).
     */
    @GetMapping("/facturas/excel")
    public ResponseEntity<byte[]> descargarReporteExcel() throws IOException {
        byte[] excelBytes = reporteService.generarReporteExcelBytes();
        String filename = "reporte_facturas_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

        MediaType excelMediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(excelMediaType)
                .contentLength(excelBytes.length)
                .body(excelBytes);
    }
}
