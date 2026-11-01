package com.facturaarca.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Cuerpo del pedido para informar una o varias facturas a ARCA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InformarArcaSolicitudDTO {

    @NotEmpty(message = "Debe indicar al menos una factura para informar")
    private List<@NotNull(message = "Los ids de factura no pueden ser nulos") Long> ids;
}
