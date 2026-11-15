package com.facturaarca.controllers;

import com.facturaarca.dto.ArticuloPrecioDTO;
import com.facturaarca.dto.ListaPrecioArticuloDTO;
import com.facturaarca.dto.RespuestaPaginadaDTO;
import com.facturaarca.repository.ArticuloRepository;
import com.facturaarca.repository.ListaPrecioArticuloRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/articulos")
@CrossOrigin(origins = "*")
public class ArticuloRestController {

    private final ArticuloRepository articuloRepository;
    private final ListaPrecioArticuloRepository listaPrecioArticuloRepository;

    public ArticuloRestController(ArticuloRepository articuloRepository,
            ListaPrecioArticuloRepository listaPrecioArticuloRepository) {
        this.articuloRepository = articuloRepository;
        this.listaPrecioArticuloRepository = listaPrecioArticuloRepository;
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
        String qCodigo = codigo != null ? codigo.trim() : "";
        String qTermino = termino != null ? termino.trim() : "";
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));

        Page<ArticuloPrecioDTO> resultado = articuloRepository
                .buscarPaginadoPorListaPrecio(qCodigo, qTermino, listaPrecioId, pageable);

        RespuestaPaginadaDTO<ArticuloPrecioDTO> respuesta = RespuestaPaginadaDTO.<ArticuloPrecioDTO>builder()
                .contenido(resultado.getContent())
                .paginaActual(resultado.getNumber())
                .tamanioPagina(resultado.getSize())
                .totalElementos(resultado.getTotalElements())
                .totalPaginas(resultado.getTotalPages())
                .build();

        return ResponseEntity.ok(respuesta);
    }

    /**
     * Listas de precios disponibles para un artículo.
     * Lo usa el formulario de alta para que el usuario elija con qué lista
     * se cotiza cada detalle.
     */
    @GetMapping("/{id}/listas-precio")
    public List<ListaPrecioArticuloDTO> listasPrecio(@PathVariable Long id) {
        if (!articuloRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Artículo no encontrado con id: " + id);
        }
        return listaPrecioArticuloRepository.buscarListasPrecioPorArticulo(id);
    }
}
