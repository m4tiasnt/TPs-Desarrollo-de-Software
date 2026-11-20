package com.facturaarca.controllers;

import com.facturaarca.dto.FacturaVentaDTO;
import com.facturaarca.dto.FacturaVentaRequestDTO;
import com.facturaarca.dto.FacturaVentaListadoDTO;
import com.facturaarca.dto.ResultadoAutorizacionDTO;
import com.facturaarca.dto.RespuestaPaginadaDTO;
import com.facturaarca.enums.EstadoFactura;
import com.facturaarca.services.FacturaPdfService;
import com.facturaarca.services.FacturaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/facturas")
public class FacturaRestController {

    private final FacturaService facturaService;
    private final FacturaPdfService facturaPdfService;

    public FacturaRestController(FacturaService facturaService, FacturaPdfService facturaPdfService) {
        this.facturaService = facturaService;
        this.facturaPdfService = facturaPdfService;
    }

    @GetMapping
    public RespuestaPaginadaDTO<FacturaVentaListadoDTO> getFacturas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoFactura estado,
            @RequestParam(required = false) Double montoMinimo,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean informadaArca,
            @RequestParam(value = "pagina", defaultValue = "0")
            @Min(value = 0, message = "El número de página no puede ser negativo") int pagina,
            @RequestParam(value = "tamanio", defaultValue = "20")
            @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
            @Max(value = 100, message = "El tamaño de página no puede ser superior a 100") int tamanio) {

        return facturaService.buscarFacturasPaginadas(
                fechaDesde, fechaHasta, estado, montoMinimo,
                clienteId, busqueda, informadaArca, pagina, tamanio);
    }

    @GetMapping("/{id}")
    public FacturaVentaDTO getFactura(@PathVariable Long id) {
        return facturaService.obtenerDetalle(id);
    }

    /**
     * Descarga el comprobante individual en formato PDF (A4 vertical con diseño fiscal ARCA y QR).
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarFacturaPdf(@PathVariable Long id) throws IOException {
        FacturaPdfService.PdfComprobante pdf = facturaPdfService.generarFacturaPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdf.filename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.bytes().length)
                .body(pdf.bytes());
    }

    /**
     * Guarda una factura en estado BORRADOR.
     */
    @PostMapping
    public ResponseEntity<FacturaVentaDTO> crearFactura(@Valid @RequestBody FacturaVentaRequestDTO facturaVentaRequestDTO) {
        FacturaVentaDTO facturaCreada = facturaService.crearFactura(facturaVentaRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaCreada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacturaVentaDTO> actualizarFactura(
            @PathVariable Long id,
            @Valid @RequestBody FacturaVentaRequestDTO facturaVentaRequestDTO) {
        FacturaVentaDTO facturaActualizada = facturaService.actualizarFactura(id, facturaVentaRequestDTO);
        return ResponseEntity.ok(facturaActualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarFactura(@PathVariable Long id) {
        facturaService.eliminarFactura(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/anular")
    public ResponseEntity<FacturaVentaDTO> anularFactura(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.anularFactura(id));
    }

    /**
     * Informa una factura individual ante ARCA para obtener CAE.
     */
    @PostMapping("/{id}/informar-arca")
    public ResponseEntity<ResultadoAutorizacionDTO> informarArca(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.informarArca(id));
    }

    /**
     * Informa un lote de facturas ante ARCA para obtener CAE.
     */
    @PostMapping("/informar-arca-lote")
    public ResponseEntity<List<ResultadoAutorizacionDTO>> informarArcaLote(
            @NotEmpty(message = "Indicá al menos una factura para informar a ARCA") @RequestBody List<Long> ids) {
        return ResponseEntity.ok(facturaService.informarArcaLote(ids));
    }
}
