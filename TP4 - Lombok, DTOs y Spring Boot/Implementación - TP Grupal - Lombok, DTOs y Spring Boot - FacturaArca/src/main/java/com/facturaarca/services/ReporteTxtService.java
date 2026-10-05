package com.facturaarca.services;

import com.facturaarca.dto.FacturaReporteDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class ReporteTxtService {

    private static final String SEP = "\t";
    private static final String SALTO = "\r\n";
    private static final String BOM_UTF8 = "\uFEFF"; 
    private final FacturaService facturaService;

    @Value("${app.reportes.dir:reportes}")
    private String reportesDir;

    public ReporteTxtService(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    public Path generarReporteFacturasTxt() {
        return generarReporteFacturasTxt(facturaService.obtenerTodasLasFacturas());
    }

    
    public Path generarReporteFacturasTxt(List<FacturaReporteDTO> facturas) {
        try {
            Path dir = Paths.get(reportesDir);
            Files.createDirectories(dir);

            String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path destino = dir.resolve("reporte_facturas_" + fechaHora + ".txt");

            Files.writeString(destino, BOM_UTF8 + construirContenido(facturas), StandardCharsets.UTF_8);

            System.out.println("TXT generado exitosamente en: " + destino + ".");
            return destino;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el reporte TXT", e);
        }
    }

    public String construirContenido(List<FacturaReporteDTO> facturas) {
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
        Locale ar = Locale.forLanguageTag("es-AR");

        StringBuilder sb = new StringBuilder();

        sb.append(String.join(SEP,
                "Número", "Fecha", "Cliente", "Cond. IVA", "Pto. Venta", "Total", "Ítems"))
          .append(SALTO);


        for (FacturaReporteDTO f : facturas) {
            sb.append(f.getNumeroFactura() != null ? f.getNumeroFactura() : "").append(SEP)
              .append(f.getFechaEmision() != null ? formatoFecha.format(f.getFechaEmision()) : "").append(SEP)
              .append(limpiar(f.getClienteDenominacion())).append(SEP)
              .append(limpiar(f.getCondicionIva())).append(SEP)
              .append(limpiar(f.getPuntoVentaDescripcion())).append(SEP)
              .append(String.format(ar, "%.2f", f.getImporteTotal())).append(SEP)
              .append(f.getCantidadItems() != null ? f.getCantidadItems() : 0)
              .append(SALTO);
        }

        return sb.toString();
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.replaceAll("[\\t\\r\\n]+", " ").trim();
    }
}