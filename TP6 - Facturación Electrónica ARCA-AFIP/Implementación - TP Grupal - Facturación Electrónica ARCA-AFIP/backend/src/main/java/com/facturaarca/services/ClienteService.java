package com.facturaarca.services;

import com.facturaarca.dto.ClienteDTO;
import com.facturaarca.dto.RespuestaPaginadaDTO;
import com.facturaarca.repository.ClienteRepository;
import com.facturaarca.util.SqlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

/**
 * Servicio de consulta de clientes.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteService {

    private static final Set<String> CAMPOS_ORDEN_PERMITIDOS = Set.of("id", "denominacion", "cuitCuil");

    private final ClienteRepository clienteRepository;

    public List<ClienteDTO> listarActivos() {
        return clienteRepository.listarActivos();
    }

    public RespuestaPaginadaDTO<ClienteDTO> buscarClientes(String cuit, String razonSocial, Pageable pageable) {
        Pageable pageableSeguro = validarYSanitizarPageable(pageable);

        String cuitLimpio = cuit != null ? cuit.trim() : "";
        String qCuit = SqlUtils.escaparLike(cuitLimpio);
        String cuitSinGuiones = SqlUtils.escaparLike(cuitLimpio.replace("-", ""));
        String qRazon = SqlUtils.escaparLike(razonSocial);

        Page<ClienteDTO> resultado = clienteRepository
                .buscarPorCuitYRazonSocial(qCuit, cuitSinGuiones, qRazon, pageableSeguro);

        return RespuestaPaginadaDTO.<ClienteDTO>builder()
                .contenido(resultado.getContent())
                .paginaActual(resultado.getNumber())
                .tamanioPagina(resultado.getSize())
                .totalElementos(resultado.getTotalElements())
                .totalPaginas(resultado.getTotalPages())
                .build();
    }

    private static Pageable validarYSanitizarPageable(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!CAMPOS_ORDEN_PERMITIDOS.contains(order.getProperty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Campo de ordenamiento inválido: '" + order.getProperty() + "'");
            }
        }
        int page = Math.max(0, pageable.getPageNumber());
        int size = Math.min(100, Math.max(1, pageable.getPageSize()));
        return PageRequest.of(page, size, pageable.getSort());
    }
}
