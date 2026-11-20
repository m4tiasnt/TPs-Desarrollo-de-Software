package com.facturaarca.controllers;

import com.facturaarca.dto.ArticuloPrecioDTO;
import com.facturaarca.dto.ListaPrecioArticuloDTO;
import com.facturaarca.dto.RespuestaPaginadaDTO;
import com.facturaarca.services.CatalogoService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/articulos")
public class ArticuloRestController {

    private final CatalogoService catalogoService;

    public ArticuloRestController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    /**
     * Búsqueda paginada de artículos con precio dentro de la lista indicada.
     */
    @GetMapping("/buscar")
    public ResponseEntity<RespuestaPaginadaDTO<ArticuloPrecioDTO>> buscarArticulosPaginados(
            @RequestParam Long listaPrecioId,
            @RequestParam(required = false, defaultValue = "") String codigo,
            @RequestParam(required = false, defaultValue = "") String termino,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "El número de página no puede ser negativo") int page,
            @RequestParam(defaultValue = "5") @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
            @Max(value = 100, message = "El tamaño de página no puede ser superior a 100") int size) {
        return ResponseEntity.ok(
                catalogoService.buscarArticulosPaginados(listaPrecioId, codigo, termino, page, size));
    }

    /**
     * Listas de precios disponibles para un artículo.
     */
    @GetMapping("/{id}/listas-precio")
    public List<ListaPrecioArticuloDTO> listasPrecio(@PathVariable Long id) {
        return catalogoService.obtenerListasPrecioDeArticulo(id);
    }
}
