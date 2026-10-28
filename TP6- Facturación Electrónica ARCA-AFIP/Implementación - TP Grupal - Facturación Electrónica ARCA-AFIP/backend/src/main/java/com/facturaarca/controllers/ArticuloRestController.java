package com.facturaarca.controllers;

import com.facturaarca.dto.ListaPrecioArticuloDTO;
import com.facturaarca.repository.ArticuloRepository;
import com.facturaarca.repository.ListaPrecioArticuloRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
     * Listas de precios disponibles para un artículo.
     * Lo usa el formulario de alta para que el usuario elija con qué lista
     * se cotiza cada renglón.
     */
    @GetMapping("/{id}/listas-precio")
    public List<ListaPrecioArticuloDTO> listasPrecio(@PathVariable Long id) {
        if (!articuloRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Artículo no encontrado con id: " + id);
        }
        return listaPrecioArticuloRepository.buscarListasPrecioPorArticulo(id);
    }
}
