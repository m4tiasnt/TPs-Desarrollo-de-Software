package com.facturaarca.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cada detalle que envía el frontend al crear o editar una factura.
 * El precio y los importes los calcula el backend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaVentaDetalleRequestDTO {

    private Long articuloId;
    private Long listaPrecioArticuloId;
    private String descripcion;

    @Positive(message = "La cantidad debe ser mayor a 0")
    private double cantidad;

    @DecimalMin(value = "0.0", message = "El porcentaje de bonificación no puede ser negativo")
    @DecimalMax(value = "100.0", message = "El porcentaje de bonificación no puede superar 100")
    private Double porcentajeBonificacion;
}
