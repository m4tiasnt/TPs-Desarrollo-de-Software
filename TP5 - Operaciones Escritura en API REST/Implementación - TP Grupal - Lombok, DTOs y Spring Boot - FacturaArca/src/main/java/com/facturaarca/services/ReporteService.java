package com.facturaarca.services;

import com.facturaarca.dto.FacturaReporteDTO;
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

import java.io.FileOutputStream;
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

    public Path generarReportePdf() throws IOException {
        List<FacturaReporteDTO> facturas = facturaService.obtenerTodasLasFacturas();

        // Buscamos o creamos el directorio donde se guardará el PDF
        Path dir = Paths.get(reportesDir);
        Files.createDirectories(dir);

        // Generamos un nombre de archivo único basado en la fecha y hora actual
        String fechaHoraActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path destino = dir.resolve("reporte_facturas_" + fechaHoraActual + ".pdf");

        // Inicializamos el escritor y el documento PDF
        PdfWriter writer = new PdfWriter(destino.toString());
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
                // 4. Creamos una tabla con 7 columnas (una por cada atributo del DTO)
                float[] columnWidths = {50F, 85F, 100F, 115F, 100F, 80F, 50F};
                Table table = new Table(columnWidths);
                table.setWidth(UnitValue.createPercentValue(100));

                // 5. Agregamos los encabezados de la tabla
                table.addHeaderCell(headerCell("Número", negrita).setTextAlignment(TextAlignment.RIGHT));
                table.addHeaderCell(headerCell("Fecha", negrita));
                table.addHeaderCell(headerCell("Cliente", negrita));
                table.addHeaderCell(headerCell("Cond. IVA", negrita));
                table.addHeaderCell(headerCell("Pto. Venta", negrita));
                table.addHeaderCell(headerCell("Total", negrita).setTextAlignment(TextAlignment.RIGHT));
                table.addHeaderCell(headerCell("Ítems", negrita).setTextAlignment(TextAlignment.RIGHT));

                // 6. Recorremos la lista de DTOs y llenamos la tabla con los datos reales
                SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
                Locale formatoArgentino = Locale.forLanguageTag("es-AR");
                for (FacturaReporteDTO factura : facturas) {
                    table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                            factura.getNumeroFactura() != null ? factura.getNumeroFactura().toString() : "")
                            .setTextAlignment(TextAlignment.RIGHT)));
                    table.addCell(formatoFecha.format(factura.getFechaEmision()));
                    table.addCell(factura.getClienteDenominacion());
                    table.addCell(factura.getCondicionIva());
                    table.addCell(factura.getPuntoVentaDescripcion() != null ? factura.getPuntoVentaDescripcion() : "");
                    table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("$ " + String.format(formatoArgentino, "%,.2f", factura.getImporteTotal()))
                            .setTextAlignment(TextAlignment.RIGHT)));
                    table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(factura.getCantidadItems().toString())
                            .setTextAlignment(TextAlignment.RIGHT)));
                }

                // 7. Agregamos la tabla al documento
                document.add(table);

                // Pie con la fecha de generación, abajo a la derecha en la última página
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