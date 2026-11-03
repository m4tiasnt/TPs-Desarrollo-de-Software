package com.facturaarca.controllers;

import com.facturaarca.dto.FacturaVentaDTO;
import com.facturaarca.dto.FacturaVentaRequestDTO;
import com.facturaarca.dto.FacturaVentaListadoDTO;
import com.facturaarca.dto.ResultadoAutorizacionDTO;
import com.facturaarca.dto.RespuestaPaginadaDTO;
import com.facturaarca.enums.EstadoFactura;
import com.facturaarca.services.FacturaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Validated
@RestController
@RequestMapping("/api/facturas")
@CrossOrigin(origins = "*")
public class FacturaRestController {

    private final FacturaService facturaService;

    public FacturaRestController(FacturaService facturaService) {
        this.facturaService = facturaService;
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
     * Informa una factura individual ante ARCA / AFIP para obtener CAE.
     */
    @PostMapping("/{id}/informar-arca")
    public ResponseEntity<ResultadoAutorizacionDTO> informarArca(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.informarArca(id));
    }
}
