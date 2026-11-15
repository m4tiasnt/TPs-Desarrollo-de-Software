package com.facturaarca.controllers;

import com.facturaarca.dto.ClienteDTO;
import com.facturaarca.dto.RespuestaPaginadaDTO;
import com.facturaarca.repository.ClienteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteRestController {

    private final ClienteRepository clienteRepository;

    public ClienteRestController(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @GetMapping("/buscar")
    public ResponseEntity<RespuestaPaginadaDTO<ClienteDTO>> listarClientes(
            @RequestParam(required = false, defaultValue = "") String cuit,
            @RequestParam(required = false, defaultValue = "") String razonSocial,
            @RequestParam(required = false, defaultValue = "") String filtro,
            @PageableDefault(page = 0, size = 10, sort = "denominacion") Pageable pageable) {
        String qCuit = (cuit != null) ? cuit.trim() : "";
        String cuitSinGuiones = qCuit.replace("-", "").trim();
        String qRazon = (razonSocial != null) ? razonSocial.trim() : "";
        String qFiltro = (filtro != null) ? filtro.trim() : "";

        Page<ClienteDTO> resultado;
        if (qCuit.isEmpty() && qRazon.isEmpty() && !qFiltro.isEmpty()) {
            resultado = clienteRepository
                    .findByDenominacionOrCuitCuil(qFiltro, pageable)
                    .map(c -> new ClienteDTO(c.getId(), c.getCuitCuil(), c.getDenominacion()));
        } else {
            resultado = clienteRepository
                    .buscarPorCuitYRazonSocial(qCuit, cuitSinGuiones, qRazon, pageable)
                    .map(c -> new ClienteDTO(c.getId(), c.getCuitCuil(), c.getDenominacion()));
        }

        RespuestaPaginadaDTO<ClienteDTO> respuesta = RespuestaPaginadaDTO.<ClienteDTO>builder()
                .contenido(resultado.getContent())
                .paginaActual(resultado.getNumber())
                .tamanioPagina(resultado.getSize())
                .totalElementos(resultado.getTotalElements())
                .totalPaginas(resultado.getTotalPages())
                .build();

        return ResponseEntity.ok(respuesta);
    }
}
