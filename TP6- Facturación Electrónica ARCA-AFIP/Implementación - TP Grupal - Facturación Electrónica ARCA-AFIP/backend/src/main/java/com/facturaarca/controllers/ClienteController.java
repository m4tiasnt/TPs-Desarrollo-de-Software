package com.facturaarca.controllers;

import com.facturaarca.dto.ClienteDTO;
import com.facturaarca.repository.ClienteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    private final ClienteRepository clienteRepository;

    public ClienteController(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @GetMapping("/buscar")
    public ResponseEntity<Page<ClienteDTO>> listarClientes(
            @RequestParam(required = false, defaultValue = "") String filtro,
            @PageableDefault(page = 0, size = 10, sort = "denominacion") Pageable pageable) {
        String q = (filtro != null) ? filtro.trim() : "";
        Page<ClienteDTO> resultado = clienteRepository
                .findByDenominacionOrCuitCuil(q, pageable)
                .map(c -> new ClienteDTO(c.getId(), c.getCuitCuil(), c.getDenominacion()));
        return ResponseEntity.ok(resultado);
    }
}
