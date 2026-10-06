package com.facturaarca.services;

import com.facturaarca.dto.FacturaReporteDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReporteExcelService {

    private final FacturaService facturaService;

    @Value("${app.reportes.dir:reportes}")
    private String reportesDir;

    public ReporteExcelService(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        try {
            generarReporteExcel();
        } catch (Exception e) {
            System.err.println("No se pudo generar el Excel al arrancar: " + e.getMessage());
        }
    }

    public Path generarReporteExcel() throws IOException {
        List<FacturaReporteDTO> facturas = facturaService.obtenerTodasLasFacturas();

        Path dir = Paths.get(reportesDir);
        Files.createDirectories(dir);

        String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path destino = dir.resolve("reporte_facturas_" + fechaHora + ".xlsx");

        // Creamos el archivo Excel real en memoria
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Facturas");

            // Estilo para la cabecera (Negrita y fondo gris)
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Estilo para moneda ($)
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("$#,##0.00"));

            // Estilo para fecha
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(format.getFormat("dd/MM/yyyy"));

            // Crear fila de cabecera
            Row headerRow = sheet.createRow(0);
            String[] columnas = {"Número", "Fecha", "Cliente", "Cond. IVA", "Pto. Venta", "Total", "Ítems"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            // Llenar datos de facturas
            int rowNum = 1;
            for (FacturaReporteDTO f : facturas) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(f.getNumeroFactura() != null ? f.getNumeroFactura() : 0);
                
                Cell dateCell = row.createCell(1);
                if (f.getFechaEmision() != null) {
                    dateCell.setCellValue(f.getFechaEmision());
                    dateCell.setCellStyle(dateStyle);
                }

                row.createCell(2).setCellValue(limpiar(f.getClienteDenominacion()));
                row.createCell(3).setCellValue(limpiar(f.getCondicionIva()));
                row.createCell(4).setCellValue(limpiar(f.getPuntoVentaDescripcion()));

                 Cell totalCell = row.createCell(5);
                 totalCell.setCellValue(f.getImporteTotal());
                  totalCell.setCellStyle(currencyStyle);

                row.createCell(6).setCellValue(f.getCantidadItems() != null ? f.getCantidadItems() : 0);
            }

            // Autoajustar el ancho de las columnas
            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Escribir el archivo físico en el disco
            try (FileOutputStream fileOut = new FileOutputStream(destino.toFile())) {
                workbook.write(fileOut);
            }
        }

        System.out.println("Excel (.xlsx) generado exitosamente en: " + destino + ".");
        return destino;
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.replaceAll("[\\t\\r\\n]+", " ").trim();
    }
}