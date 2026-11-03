package com.facturaarca.controllers;

import com.facturaarca.dto.FacturaVentaDTO;
import com.facturaarca.dto.FacturaVentaRequestDTO;
import com.facturaarca.dto.FacturaVentaListadoDTO;
import com.facturaarca.dto.InformarArcaResultadoDTO;
import com.facturaarca.dto.InformarArcaSolicitudDTO;
import com.facturaarca.dto.RespuestaPaginadaDTO;
import com.facturaarca.enums.EstadoFactura;
import com.facturaarca.services.FacturaService;
import com.facturaarca.services.FacturacionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/facturas")
@CrossOrigin(origins = "*")
public class FacturaRestController {

    private final FacturaService facturaService;
    private final FacturacionService facturacionService;

    public FacturaRestController(FacturaService facturaService, FacturacionService facturacionService) {
        this.facturaService = facturaService;
        this.facturacionService = facturacionService;
    }

    @GetMapping
    public RespuestaPaginadaDTO<FacturaVentaListadoDTO> getFacturas(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaHasta,
            @RequestParam(required = false) EstadoFactura estado,
            @RequestParam(required = false) Double montoMinimo,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean informadaArca,
            @RequestParam(value = "pagina", defaultValue = "0") int pagina,
            @RequestParam(value = "tamanio", defaultValue = "20") int tamanio) {

        return facturaService.buscarFacturasPaginadas(
                fechaDesde, fechaHasta, estado, montoMinimo,
                clienteId, busqueda, informadaArca, pagina, tamanio);
    }

    @GetMapping("/{id}")
    public FacturaVentaDTO getFactura(@PathVariable Long id) {
        return facturaService.obtenerDetalle(id);
    }

    /**
     * Guarda una factura en estado BORRADOR.
     */
    @PostMapping
    public ResponseEntity<FacturaVentaDTO> crearFactura(@Valid @RequestBody FacturaVentaRequestDTO facturaVentaRequestDTO) {
        FacturaVentaDTO facturaCreada = facturaService.crearFactura(facturaVentaRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaCreada);
    }

    /**
     * Emite una factura directamente con ARCA / AFIP:
     * Solicita Token y Sign a WSAA, consulta último comprobante en WSFEv1,
     * pide CAE a AFIP y persiste la factura AUTORIZADA en PostgreSQL.
     */
    @PostMapping("/emitir")
    public ResponseEntity<FacturaVentaDTO> emitirFactura(@Valid @RequestBody FacturaVentaRequestDTO facturaVentaRequestDTO) {
        FacturaVentaDTO facturaEmitida = facturacionService.emitirFactura(facturaVentaRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaEmitida);
    }

    /**
     * Emite una factura previamente guardada en estado BORRADOR con ARCA / AFIP.
     */
    @PostMapping("/{id}/emitir")
    public ResponseEntity<FacturaVentaDTO> emitirFacturaExistente(@PathVariable Long id) {
        FacturaVentaDTO facturaEmitida = facturacionService.emitirFacturaExistente(id);
        return ResponseEntity.ok(facturaEmitida);
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

    @PostMapping("/informar-arca")
    public ResponseEntity<List<InformarArcaResultadoDTO>> informarArca(
            @Valid @RequestBody InformarArcaSolicitudDTO solicitud) {
        return ResponseEntity.ok(facturaService.informarArca(solicitud.getIds()));
    }

    @PostMapping("/{id}/informar-arca")
    public ResponseEntity<List<InformarArcaResultadoDTO>> informarArcaUna(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.informarArca(List.of(id)));
    }
}
