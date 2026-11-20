package com.facturaarca.services;

import com.facturaarca.dto.FacturaVentaReporteDTO;
import com.facturaarca.enums.EstadoFactura;
import com.facturaarca.util.FormatoComprobante;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class ReporteService {

    private final FacturaService facturaService;

    public ReporteService(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    /**
     * Genera el reporte Excel en memoria retornando los bytes listos para descarga HTTP.
     */
    public byte[] generarReporteExcelBytes(
            LocalDate fechaDesde, LocalDate fechaHasta, EstadoFactura estado, Double montoMinimo,
            Long clienteId, String busqueda, Boolean informadaArca) throws IOException {
        List<FacturaVentaReporteDTO> facturas = facturaService.buscarFacturasParaReporte(
                fechaDesde, fechaHasta, estado, montoMinimo, clienteId, busqueda, informadaArca);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Facturas");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("$#,##0.00"));

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(format.getFormat("dd/MM/yyyy"));

            Row headerRow = sheet.createRow(0);
            String[] columnas = {"Número", "Fecha", "Cliente", "Cond. IVA", "Pto. Venta", "Total", "Ítems", "Estado"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (FacturaVentaReporteDTO f : facturas) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(formatearIdentificador(f));

                Cell dateCell = row.createCell(1);
                if (f.getFechaEmision() != null) {
                    dateCell.setCellValue(f.getFechaEmision());
                    dateCell.setCellStyle(dateStyle);
                }

                row.createCell(2).setCellValue(limpiar(f.getClienteDenominacion()));
                row.createCell(3).setCellValue(limpiar(f.getCondicionIva()));
                row.createCell(4).setCellValue(limpiar(formatearPuntoVenta(f)));

                Cell totalCell = row.createCell(5);
                totalCell.setCellValue(f.getImporteTotal());
                totalCell.setCellStyle(currencyStyle);

                row.createCell(6).setCellValue(f.getCantidadItems() != null ? f.getCantidadItems() : 0);
                row.createCell(7).setCellValue(f.getEstado() != null ? f.getEstado().name() : "");
            }

            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Genera el reporte PDF en memoria retornando los bytes listos para descarga HTTP.
     */
    public byte[] generarReportePdfBytes(
            LocalDate fechaDesde, LocalDate fechaHasta, EstadoFactura estado, Double montoMinimo,
            Long clienteId, String busqueda, Boolean informadaArca) throws IOException {
        List<FacturaVentaReporteDTO> facturas = facturaService.buscarFacturasParaReporte(
                fechaDesde, fechaHasta, estado, montoMinimo, clienteId, busqueda, informadaArca);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4.rotate());
        document.setMargins(28, 28, 28, 28);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont negrita = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        DeviceRgb colorHeader = new DeviceRgb(30, 41, 59);
        DeviceRgb colorZebra = new DeviceRgb(248, 250, 252);
        DeviceRgb colorBorde = new DeviceRgb(226, 232, 240);
        DeviceRgb colorSubtitulo = new DeviceRgb(100, 116, 139);
        Border bordeFila = new SolidBorder(colorBorde, 0.6f);

        try {
            document.add(new Paragraph("Reporte de Facturas")
                    .setFont(negrita).setFontSize(16)
                    .setFontColor(colorHeader)
                    .setMarginBottom(2));

            String subtitulo = "Generado el "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + " · " + facturas.size() + (facturas.size() == 1 ? " comprobante" : " comprobantes");
            document.add(new Paragraph(subtitulo)
                    .setFont(regular).setFontSize(9.5f)
                    .setFontColor(colorSubtitulo)
                    .setMarginBottom(12));

            if (facturas.isEmpty()) {
                document.add(new Paragraph("No hay facturas para mostrar.")
                        .setFont(regular).setFontSize(10.5f)
                        .setFontColor(colorSubtitulo)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(24));
            } else {
                Table table = new Table(UnitValue.createPercentArray(new float[]{11, 10, 21, 16, 13, 12, 5, 12}));
                table.setWidth(UnitValue.createPercentValue(100));

                table.addHeaderCell(headerCell("Número", negrita, colorHeader, TextAlignment.RIGHT));
                table.addHeaderCell(headerCell("Fecha", negrita, colorHeader, TextAlignment.CENTER));
                table.addHeaderCell(headerCell("Cliente", negrita, colorHeader, TextAlignment.LEFT));
                table.addHeaderCell(headerCell("Cond. IVA", negrita, colorHeader, TextAlignment.LEFT));
                table.addHeaderCell(headerCell("Pto. Venta", negrita, colorHeader, TextAlignment.LEFT));
                table.addHeaderCell(headerCell("Total", negrita, colorHeader, TextAlignment.RIGHT));
                table.addHeaderCell(headerCell("Ítems", negrita, colorHeader, TextAlignment.RIGHT));
                table.addHeaderCell(headerCell("Estado", negrita, colorHeader, TextAlignment.CENTER));

                SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
                Locale formatoArgentino = Locale.forLanguageTag("es-AR");
                int idx = 0;
                for (FacturaVentaReporteDTO factura : facturas) {
                    DeviceRgb fondo = (idx++ % 2 == 1) ? colorZebra : null;
                    String numeroTxt = formatearIdentificador(factura);
                    String fechaTxt = factura.getFechaEmision() != null ? formatoFecha.format(factura.getFechaEmision()) : "—";
                    String clienteTxt = factura.getClienteDenominacion() != null ? factura.getClienteDenominacion() : "Consumidor Final";
                    String condIvaTxt = factura.getCondicionIva() != null ? factura.getCondicionIva() : "—";
                    String ptoVtaTxt = formatearPuntoVenta(factura);
                    String totalTxt = "$ " + String.format(formatoArgentino, "%,.2f", factura.getImporteTotal());
                    String itemsTxt = factura.getCantidadItems() != null ? factura.getCantidadItems().toString() : "0";
                    String estadoTxt = factura.getEstado() != null ? factura.getEstado().name() : "";

                    table.addCell(bodyCell(numeroTxt, regular, TextAlignment.RIGHT, bordeFila, fondo));
                    table.addCell(bodyCell(fechaTxt, regular, TextAlignment.CENTER, bordeFila, fondo));
                    table.addCell(bodyCell(clienteTxt, regular, TextAlignment.LEFT, bordeFila, fondo));
                    table.addCell(bodyCell(condIvaTxt, regular, TextAlignment.LEFT, bordeFila, fondo));
                    table.addCell(bodyCell(ptoVtaTxt, regular, TextAlignment.LEFT, bordeFila, fondo));
                    table.addCell(bodyCell(totalTxt, negrita, TextAlignment.RIGHT, bordeFila, fondo));
                    table.addCell(bodyCell(itemsTxt, regular, TextAlignment.RIGHT, bordeFila, fondo));
                    table.addCell(bodyCell(estadoTxt, regular, TextAlignment.CENTER, bordeFila, fondo));
                }

                document.add(table);
            }
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private static String formatearIdentificador(FacturaVentaReporteDTO f) {
        if (f.getNumeroFactura() != null) {
            return FormatoComprobante.formatearNumeroComprobante(f.getPuntoVentaNumero(), f.getNumeroFactura());
        }
        return f.getId() != null ? "Borrador #" + f.getId() : "—";
    }

    private static String formatearPuntoVenta(FacturaVentaReporteDTO f) {
        String desc = f.getPuntoVentaDescripcion() != null ? f.getPuntoVentaDescripcion().trim() : "";
        if (f.getPuntoVentaNumero() != null) {
            String num = FormatoComprobante.formatearPuntoVenta(f.getPuntoVentaNumero());
            return desc.isEmpty() ? num : num + " · " + desc;
        }
        return desc.isEmpty() ? "—" : desc;
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.replaceAll("[\\t\\r\\n]+", " ").trim();
    }

    private static com.itextpdf.layout.element.Cell headerCell(
            String texto, PdfFont negrita, DeviceRgb fondo, TextAlignment alineacion) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(texto).setFont(negrita).setFontSize(9.5f).setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(fondo)
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(6f)
                .setPaddingBottom(6f)
                .setPaddingLeft(6f)
                .setPaddingRight(6f)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(alineacion);
    }

    private static com.itextpdf.layout.element.Cell bodyCell(
            String texto, PdfFont font, TextAlignment alineacion, Border bordeInferior, DeviceRgb fondo) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(texto).setFont(font).setFontSize(9f))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(bordeInferior)
                .setPaddingTop(5f)
                .setPaddingBottom(5f)
                .setPaddingLeft(6f)
                .setPaddingRight(6f)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(alineacion);
        if (fondo != null) {
            cell.setBackgroundColor(fondo);
        }
        return cell;
    }
}
