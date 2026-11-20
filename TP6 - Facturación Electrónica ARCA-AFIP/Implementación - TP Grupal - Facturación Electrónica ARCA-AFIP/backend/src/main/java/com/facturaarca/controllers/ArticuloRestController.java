package com.facturaarca.controllers;

import com.facturaarca.dto.ArticuloPrecioDTO;
import com.facturaarca.dto.ListaPrecioArticuloDTO;
import com.facturaarca.dto.RespuestaPaginadaDTO;
import com.facturaarca.services.CatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
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
