package com.facturaarca.controllers;

import com.facturaarca.dto.ArticuloPrecioDTO;
import com.facturaarca.dto.ClienteDTO;
import com.facturaarca.dto.CondicionIvaDTO;
import com.facturaarca.dto.ListaPrecioDTO;
import com.facturaarca.dto.PuntoVentaDTO;
import com.facturaarca.dto.TipoMonedaDTO;
import com.facturaarca.services.CatalogoService;
import com.facturaarca.services.ClienteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogoRestController {

    private final CatalogoService catalogoService;
    private final ClienteService clienteService;

    public CatalogoRestController(CatalogoService catalogoService, ClienteService clienteService) {
        this.catalogoService = catalogoService;
        this.clienteService = clienteService;
    }

    @GetMapping("/listas-precio")
    public List<ListaPrecioDTO> listasPrecio() {
        return catalogoService.listarListasPrecio();
    }

    @GetMapping("/articulos")
    public List<ArticuloPrecioDTO> buscarArticulos(
            @RequestParam(required = false) String search,
            @RequestParam Long listaPrecioId) {
        return catalogoService.buscarArticulos(search, listaPrecioId);
    }

    @GetMapping("/clientes")
    public List<ClienteDTO> clientes() {
        return clienteService.listarActivos();
    }

    @GetMapping("/puntos-venta")
    public List<PuntoVentaDTO> puntosVenta() {
        return catalogoService.listarPuntosVenta();
    }

    @GetMapping("/condiciones-iva")
    public List<CondicionIvaDTO> condicionesIva() {
        return catalogoService.listarCondicionesIva();
    }

    @GetMapping("/tipos-moneda")
    public List<TipoMonedaDTO> tiposMoneda() {
        return catalogoService.listarTiposMoneda();
    }
}
