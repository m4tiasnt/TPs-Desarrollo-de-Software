package com.facturaarca.controllers;

import com.facturaarca.dto.ClienteDTO;
import com.facturaarca.dto.RespuestaPaginadaDTO;
import com.facturaarca.services.ClienteService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
public class ClienteRestController {

    private final ClienteService clienteService;

    public ClienteRestController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping("/buscar")
    public ResponseEntity<RespuestaPaginadaDTO<ClienteDTO>> listarClientes(
            @RequestParam(required = false, defaultValue = "") String cuit,
            @RequestParam(required = false, defaultValue = "") String razonSocial,
            @PageableDefault(page = 0, size = 10, sort = "denominacion") Pageable pageable) {
        return ResponseEntity.ok(clienteService.buscarClientes(cuit, razonSocial, pageable));
    }
}
