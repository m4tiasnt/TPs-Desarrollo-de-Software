package com.facturaarca.services;

import com.facturaarca.dto.ClienteDTO;
import com.facturaarca.dto.RespuestaPaginadaDTO;
import com.facturaarca.repository.ClienteRepository;
import com.facturaarca.util.SqlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de consulta de clientes.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public List<ClienteDTO> listarActivos() {
        return clienteRepository.listarActivos();
    }

    public RespuestaPaginadaDTO<ClienteDTO> buscarClientes(String cuit, String razonSocial, Pageable pageable) {
        String cuitLimpio = cuit != null ? cuit.trim() : "";
        String qCuit = SqlUtils.escaparLike(cuitLimpio);
        String cuitSinGuiones = SqlUtils.escaparLike(cuitLimpio.replace("-", ""));
        String qRazon = SqlUtils.escaparLike(razonSocial);

        Page<ClienteDTO> resultado = clienteRepository
                .buscarPorCuitYRazonSocial(qCuit, cuitSinGuiones, qRazon, pageable);

        return RespuestaPaginadaDTO.<ClienteDTO>builder()
                .contenido(resultado.getContent())
                .paginaActual(resultado.getNumber())
                .tamanioPagina(resultado.getSize())
                .totalElementos(resultado.getTotalElements())
                .totalPaginas(resultado.getTotalPages())
                .build();
    }
}
