package com.facturaarca.services;

import com.facturaarca.dto.FacturaVentaReporteDTO;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class ReporteService {

    private final FacturaService facturaService;

    @Value("${app.reportes.dir:reportes}")
    private String reportesDir;

    public ReporteService(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    /**
     * Genera el reporte Excel en memoria retornando los bytes listos para descarga HTTP.
     */
    public byte[] generarReporteExcelBytes() throws IOException {
        List<FacturaVentaReporteDTO> facturas = facturaService.obtenerTodasLasFacturas();

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
            String[] columnas = {"Número", "Fecha", "Cliente", "Cond. IVA", "Pto. Venta", "Total", "Ítems", "Estado"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            // Llenar datos de facturas
            int rowNum = 1;
            for (FacturaVentaReporteDTO f : facturas) {
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
                row.createCell(7).setCellValue(f.getEstado() != null ? f.getEstado().name() : "");
            }

            // Autoajustar el ancho de las columnas
            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Genera el reporte Excel y lo guarda en disco (útil para auditoría o procesos batch).
     */
    public Path generarReporteExcel() throws IOException {
        byte[] bytes = generarReporteExcelBytes();
        Path dir = Paths.get(reportesDir);
        Files.createDirectories(dir);

        String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path destino = dir.resolve("reporte_facturas_" + fechaHora + ".xlsx");
        Files.write(destino, bytes);

        System.out.println("Excel (.xlsx) generado exitosamente en: " + destino + ".");
        return destino;
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.replaceAll("[\\t\\r\\n]+", " ").trim();
    }

    /**
     * Genera el reporte PDF en memoria retornando los bytes listos para descarga HTTP.
     */
    public byte[] generarReportePdfBytes() throws IOException {
        List<FacturaVentaReporteDTO> facturas = facturaService.obtenerTodasLasFacturas();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4.rotate()); // apaisado
        PdfFont negrita = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        try {
            document.add(new Paragraph("Reporte de Facturas")
                    .setFont(negrita).setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER));

            if (facturas.isEmpty()) {
                document.add(new Paragraph("No hay facturas para mostrar.")
                        .setTextAlignment(TextAlignment.CENTER));
            } else {
                float[] columnWidths = {45F, 75F, 95F, 105F, 90F, 75F, 45F, 70F};
                Table table = new Table(columnWidths);
                table.setWidth(UnitValue.createPercentValue(100));

                table.addHeaderCell(headerCell("Número", negrita).setTextAlignment(TextAlignment.RIGHT));
                table.addHeaderCell(headerCell("Fecha", negrita));
                table.addHeaderCell(headerCell("Cliente", negrita));
                table.addHeaderCell(headerCell("Cond. IVA", negrita));
                table.addHeaderCell(headerCell("Pto. Venta", negrita));
                table.addHeaderCell(headerCell("Total", negrita).setTextAlignment(TextAlignment.RIGHT));
                table.addHeaderCell(headerCell("Ítems", negrita).setTextAlignment(TextAlignment.RIGHT));
                table.addHeaderCell(headerCell("Estado", negrita));

                SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
                Locale formatoArgentino = Locale.forLanguageTag("es-AR");
                for (FacturaVentaReporteDTO factura : facturas) {
                    table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                            factura.getNumeroFactura() != null ? factura.getNumeroFactura().toString() : "")
                            .setTextAlignment(TextAlignment.RIGHT)));
                    table.addCell(factura.getFechaEmision() != null ? formatoFecha.format(factura.getFechaEmision()) : "");
                    table.addCell(factura.getClienteDenominacion() != null ? factura.getClienteDenominacion() : "Consumidor Final");
                    table.addCell(factura.getCondicionIva());
                    table.addCell(factura.getPuntoVentaDescripcion() != null ? factura.getPuntoVentaDescripcion() : "");
                    table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("$ " + String.format(formatoArgentino, "%,.2f", factura.getImporteTotal()))
                            .setTextAlignment(TextAlignment.RIGHT)));
                    table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(factura.getCantidadItems().toString())
                            .setTextAlignment(TextAlignment.RIGHT)));
                    table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                            factura.getEstado() != null ? factura.getEstado().name() : ""))
                            .setTextAlignment(TextAlignment.CENTER));
                }

                document.add(table);

                String generadoEl = "Generado el "
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                float anchoPie = 250F;
                float paginaAncho = pdf.getPage(pdf.getNumberOfPages()).getPageSize().getWidth();
                document.add(new Paragraph(generadoEl)
                        .setFontSize(10)
                        .setFixedPosition(pdf.getNumberOfPages(), paginaAncho - 36 - anchoPie, 30, anchoPie)
                        .setTextAlignment(TextAlignment.RIGHT));
            }
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    /**
     * Genera el reporte PDF y lo guarda en disco (útil para auditoría o procesos batch).
     */
    public Path generarReportePdf() throws IOException {
        byte[] bytes = generarReportePdfBytes();
        Path dir = Paths.get(reportesDir);
        Files.createDirectories(dir);

        String fechaHoraActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path destino = dir.resolve("reporte_facturas_" + fechaHoraActual + ".pdf");
        Files.write(destino, bytes);

        System.out.println("PDF generado exitosamente en: " + destino + ".");
        return destino;
    }

    private static com.itextpdf.layout.element.Cell headerCell(String texto, PdfFont negrita) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(texto).setFont(negrita))
                .setBackgroundColor(new DeviceGray(0.85f))
                .setTextAlignment(TextAlignment.CENTER);
    }
}
