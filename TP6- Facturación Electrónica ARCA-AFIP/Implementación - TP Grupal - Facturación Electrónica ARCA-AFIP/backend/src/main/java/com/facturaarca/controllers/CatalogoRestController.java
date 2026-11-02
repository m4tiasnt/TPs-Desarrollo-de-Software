package com.facturaarca.controllers;

import com.facturaarca.dto.ArticuloPrecioDTO;
import com.facturaarca.dto.ClienteDTO;
import com.facturaarca.dto.CondicionIvaDTO;
import com.facturaarca.dto.ListaPrecioDTO;
import com.facturaarca.dto.PuntoVentaDTO;
import com.facturaarca.dto.TipoMonedaDTO;
import com.facturaarca.repository.ArticuloRepository;
import com.facturaarca.repository.ClienteRepository;
import com.facturaarca.repository.CondicionIvaRepository;
import com.facturaarca.repository.ListaPrecioRepository;
import com.facturaarca.repository.PuntoVentaRepository;
import com.facturaarca.repository.TipoMonedaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CatalogoRestController {

    private static final int MINIMO_CARACTERES_BUSQUEDA = 3;

    private final ArticuloRepository articuloRepository;
    private final ClienteRepository clienteRepository;
    private final CondicionIvaRepository condicionIvaRepository;
    private final ListaPrecioRepository listaPrecioRepository;
    private final PuntoVentaRepository puntoVentaRepository;
    private final TipoMonedaRepository tipoMonedaRepository;

    public CatalogoRestController(ArticuloRepository articuloRepository,
            ClienteRepository clienteRepository,
            CondicionIvaRepository condicionIvaRepository,
            ListaPrecioRepository listaPrecioRepository,
            PuntoVentaRepository puntoVentaRepository,
            TipoMonedaRepository tipoMonedaRepository) {
        this.articuloRepository = articuloRepository;
        this.clienteRepository = clienteRepository;
        this.condicionIvaRepository = condicionIvaRepository;
        this.listaPrecioRepository = listaPrecioRepository;
        this.puntoVentaRepository = puntoVentaRepository;
        this.tipoMonedaRepository = tipoMonedaRepository;
    }

    @GetMapping("/listas-precio")
    public List<ListaPrecioDTO> listasPrecio() {
        return listaPrecioRepository.findAll().stream()
                .map(lp -> new ListaPrecioDTO(lp.getId(), lp.getCodigo(), lp.getDenominacion()))
                .toList();
    }

    /**
     * Busca artículos por código, denominación o marca dentro de una lista de
     * precios. Exige al menos 3 caracteres y no devuelve artículos que no
     * tengan precio en esa lista.
     */
    @GetMapping("/articulos")
    public List<ArticuloPrecioDTO> buscarArticulos(
            @RequestParam(required = false) String search,
            @RequestParam Long listaPrecioId) {

        String q = search != null ? search.trim() : "";
        if (q.length() < MINIMO_CARACTERES_BUSQUEDA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ingresá al menos " + MINIMO_CARACTERES_BUSQUEDA
                            + " caracteres para buscar artículos");
        }

        return articuloRepository.buscarPorTermino(q, listaPrecioId).stream().limit(15).toList();
    }

    @GetMapping("/clientes")
    public List<ClienteDTO> clientes() {
        return clienteRepository.findAll().stream()
                .map(c -> new ClienteDTO(c.getId(), c.getCuitCuil(), c.getDenominacion()))
                .toList();
    }

    @GetMapping("/puntos-venta")
    public List<PuntoVentaDTO> puntosVenta() {
        return puntoVentaRepository.findAll().stream()
                .map(pv -> new PuntoVentaDTO(pv.getId(), pv.getNumero(), pv.getDescripcion()))
                .toList();
    }

    @GetMapping("/condiciones-iva")
    public List<CondicionIvaDTO> condicionesIva() {
        return condicionIvaRepository.findAll().stream()
                .map(ci -> new CondicionIvaDTO(ci.getId(), ci.getCodigoAfip(), ci.getDenominacion()))
                .toList();
    }

    @GetMapping("/tipos-moneda")
    public List<TipoMonedaDTO> tiposMoneda() {
        return tipoMonedaRepository.findAll().stream()
                .map(tm -> new TipoMonedaDTO(tm.getId(), tm.getCodigoAfip(), tm.getDenominacion(), tm.getSimbolo()))
                .toList();
    }
}
