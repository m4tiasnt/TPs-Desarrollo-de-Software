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
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FacturaService {

    @PersistenceContext
    private EntityManager em;

    @Value("${app.reportes.dir:reportes}")
    private String reportesDir;

    public List<FacturaReporteDTO> obtenerTodasLasFacturas() {
        String jpql = """
                    SELECT new com.facturaarca.dto.FacturaReporteDTO(
                        f.numero,
                        f.fechaEmision,
                        COALESCE(c.denominacion, 'Consumidor Final'),
                        ci.denominacion,
                        pv.descripcion,
                        f.importeTotal,
                        COUNT(d)
                    )
                    FROM FacturaVenta f
                    LEFT JOIN f.cliente c
                    JOIN f.condicionIva ci
                    JOIN f.puntoVenta pv
                    JOIN f.detalles d
            GROUP BY f.id,
                     f.numero,
                     f.fechaEmision,
                     c.denominacion,
                     ci.denominacion,
                     pv.descripcion,
                     f.importeTotal
            ORDER BY f.numero
        """;

        TypedQuery<FacturaReporteDTO> query = em.createQuery(jpql, FacturaReporteDTO.class);
        return query.getResultList();
    }


    public List<FacturaReporteDTO> buscarFacturasFiltradas(Date fechaDesde, Date fechaHasta, String estado, Double montoMinimo) {
        StringBuilder jpql = new StringBuilder(
                "SELECT new com.facturaarca.dto.FacturaReporteDTO(" +
                        "f.numero, " +
                        "f.fechaEmision, " +
                        "COALESCE(c.denominacion, 'Consumidor Final'), " +
                        "ci.denominacion, " +
                        "pv.descripcion, " +
                        "f.importeTotal, " +
                        "COUNT(d)" +
                        ") " +
                        "FROM FacturaVenta f " +
                        "LEFT JOIN f.cliente c " +
                        "JOIN f.condicionIva ci " +
                        "JOIN f.puntoVenta pv " +
                        "JOIN f.detalles d " +
                        "WHERE 1=1 "
        );

        Map<String, Object> params = new HashMap<>();

        if (fechaDesde != null) {
            jpql.append("AND f.fechaEmision >= :fechaDesde ");
            params.put("fechaDesde", fechaDesde);
        }
        if (fechaHasta != null) {
            jpql.append("AND f.fechaEmision <= :fechaHasta ");
            params.put("fechaHasta", fechaHasta);
        }
        if (estado != null && !estado.trim().isEmpty()) {
            jpql.append("AND f.estado = :estado ");
            params.put("estado", estado);
        }
        if (montoMinimo != null) {
            jpql.append("AND f.importeTotal >= :montoMinimo ");
            params.put("montoMinimo", montoMinimo);
        }

        jpql.append("GROUP BY f.id, f.numero, f.fechaEmision, c.denominacion, ci.denominacion, pv.descripcion, f.importeTotal");
        jpql.append(" ORDER BY f.numero");

        TypedQuery<FacturaReporteDTO> query = em.createQuery(jpql.toString(), FacturaReporteDTO.class);
        params.forEach(query::setParameter);

        return query.getResultList();
    }


    public void generarReporteFacturasPdf() {
        List<FacturaReporteDTO> facturas = obtenerTodasLasFacturas();

        try {
            // Buscamos o creamos el directorio donde se guardará el PDF
            Path dir = Paths.get(reportesDir);
            Files.createDirectories(dir);

            // Generamos un nombre de archivo único basado en la fecha y hora actual
            String fechaHoraActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String destino = dir.resolve("reporte_facturas_" + fechaHoraActual + ".pdf").toString();

            // Inicializamos el escritor y el documento PDF
            PdfWriter writer = new PdfWriter(destino);
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
                        table.addCell(new Cell().add(new Paragraph(
                                factura.getNumeroFactura() != null ? factura.getNumeroFactura().toString() : "")
                                .setTextAlignment(TextAlignment.RIGHT)));
                        table.addCell(formatoFecha.format(factura.getFechaEmision()));
                        table.addCell(factura.getClienteDenominacion());
                        table.addCell(factura.getCondicionIva());
                        table.addCell(factura.getPuntoVentaDescripcion() != null ? factura.getPuntoVentaDescripcion() : "");
                        table.addCell(new Cell().add(new Paragraph("$ " + String.format(formatoArgentino, "%,.2f", factura.getImporteTotal()))
                                .setTextAlignment(TextAlignment.RIGHT)));
                        table.addCell(new Cell().add(new Paragraph(factura.getCantidadItems().toString())
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
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el reporte PDF", e);
        }
    }

    private static Cell headerCell(String texto, PdfFont negrita) {
        return new Cell()
                .add(new Paragraph(texto).setFont(negrita))
                .setBackgroundColor(new DeviceGray(0.85f))
                .setTextAlignment(TextAlignment.CENTER);
    }
}
