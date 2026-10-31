package com.facturaarca.controllers;

import com.facturaarca.dto.FacturaReporteDTO;
import com.facturaarca.dto.FacturaRequestDTO;
import com.facturaarca.dto.RespuestaPaginadaDTO;
import com.facturaarca.services.FacturaService;
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

    public FacturaRestController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @GetMapping
    public RespuestaPaginadaDTO<FacturaReporteDTO> getFacturas(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double montoMinimo,
            @RequestParam(value = "pagina", defaultValue = "0") int pagina,
            @RequestParam(value = "tamanio", defaultValue = "5") int tamanio) {

        return facturaService.buscarFacturasPaginadas(fechaDesde, fechaHasta, estado, montoMinimo, pagina, tamanio);
    }

    @PostMapping
    public ResponseEntity<FacturaReporteDTO> crearFactura(@RequestBody FacturaRequestDTO facturaRequestDTO) {
        FacturaReporteDTO facturaCreada = facturaService.crearFactura(facturaRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaCreada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacturaReporteDTO> actualizarFactura(
            @PathVariable Long id,
            @RequestBody FacturaRequestDTO facturaRequestDTO) {
        FacturaReporteDTO facturaActualizada = facturaService.actualizarFactura(id, facturaRequestDTO);
        return ResponseEntity.ok(facturaActualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarFactura(@PathVariable Long id) {
        facturaService.eliminarFactura(id);
        return ResponseEntity.noContent().build();
    }
}