package com.facturaarca.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Datos de cabecera y detalles para crear o editar una factura.
 * El estado, el número y la fecha de emisión los maneja el backend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaVentaRequestDTO {

    private Long clienteId;
    private Long condicionIvaId;
    private Long puntoVentaId;
    private Long tipoMonedaId;
    private String observaciones;

    @Builder.Default
    private List<@NotNull(message = "Cada detalle es obligatorio") @Valid FacturaVentaDetalleRequestDTO> detalles =
            new ArrayList<>();
}
