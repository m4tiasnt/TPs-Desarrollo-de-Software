package com.facturaarca.services;

import com.facturaarca.dto.ArticuloPrecioDTO;
import com.facturaarca.dto.CondicionIvaDTO;
import com.facturaarca.dto.ListaPrecioArticuloDTO;
import com.facturaarca.dto.ListaPrecioDTO;
import com.facturaarca.dto.PuntoVentaDTO;
import com.facturaarca.dto.RespuestaPaginadaDTO;
import com.facturaarca.dto.TipoMonedaDTO;
import com.facturaarca.repository.ArticuloRepository;
import com.facturaarca.repository.CondicionIvaRepository;
import com.facturaarca.repository.ListaPrecioArticuloRepository;
import com.facturaarca.repository.ListaPrecioRepository;
import com.facturaarca.repository.PuntoVentaRepository;
import com.facturaarca.repository.TipoMonedaRepository;
import com.facturaarca.util.SqlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Servicio de consulta de catálogos auxiliares (listas de precios, artículos,
 * puntos de venta, condiciones de IVA y monedas).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoService {

    private static final int MINIMO_CARACTERES_BUSQUEDA = 3;
    private static final int LIMITE_AUTOCOMPLETADO = 15;

    private final ArticuloRepository articuloRepository;
    private final CondicionIvaRepository condicionIvaRepository;
    private final ListaPrecioRepository listaPrecioRepository;
    private final ListaPrecioArticuloRepository listaPrecioArticuloRepository;
    private final PuntoVentaRepository puntoVentaRepository;
    private final TipoMonedaRepository tipoMonedaRepository;

    public List<ListaPrecioDTO> listarListasPrecio() {
        return listaPrecioRepository.findByFechaBajaIsNullOrderByIdAsc().stream()
                .map(lp -> new ListaPrecioDTO(lp.getId(), lp.getCodigo(), lp.getDenominacion()))
                .toList();
    }

    public List<PuntoVentaDTO> listarPuntosVenta() {
        return puntoVentaRepository.findByFechaBajaIsNullOrderByNumeroAsc().stream()
                .map(pv -> new PuntoVentaDTO(pv.getId(), pv.getNumero(), pv.getDescripcion()))
                .toList();
    }

    public List<CondicionIvaDTO> listarCondicionesIva() {
        return condicionIvaRepository.findByFechaBajaIsNullOrderByIdAsc().stream()
                .map(ci -> new CondicionIvaDTO(ci.getId(), ci.getCodigoAfip(), ci.getDenominacion()))
                .toList();
    }

    public List<TipoMonedaDTO> listarTiposMoneda() {
        return tipoMonedaRepository.findByFechaBajaIsNullOrderByIdAsc().stream()
                .map(tm -> new TipoMonedaDTO(tm.getId(), tm.getCodigoAfip(), tm.getDenominacion(), tm.getSimbolo()))
                .toList();
    }

    public List<ArticuloPrecioDTO> buscarArticulos(String search, Long listaPrecioId) {
        String q = search != null ? search.trim() : "";
        if (q.length() < MINIMO_CARACTERES_BUSQUEDA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ingresá al menos " + MINIMO_CARACTERES_BUSQUEDA + " caracteres para buscar artículos");
        }
        return articuloRepository.buscarPorTermino(
                SqlUtils.escaparLike(q),
                listaPrecioId,
                PageRequest.of(0, LIMITE_AUTOCOMPLETADO));
    }

    public RespuestaPaginadaDTO<ArticuloPrecioDTO> buscarArticulosPaginados(
            Long listaPrecioId, String codigo, String termino, int page, int size) {
        String qCodigo = SqlUtils.escaparLike(codigo);
        String qTermino = SqlUtils.escaparLike(termino);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));

        Page<ArticuloPrecioDTO> resultado = articuloRepository
                .buscarPaginadoPorListaPrecio(qCodigo, qTermino, listaPrecioId, pageable);

        return RespuestaPaginadaDTO.<ArticuloPrecioDTO>builder()
                .contenido(resultado.getContent())
                .paginaActual(resultado.getNumber())
                .tamanioPagina(resultado.getSize())
                .totalElementos(resultado.getTotalElements())
                .totalPaginas(resultado.getTotalPages())
                .build();
    }

    public List<ListaPrecioArticuloDTO> obtenerListasPrecioDeArticulo(Long articuloId) {
        if (!articuloRepository.existsByIdAndFechaBajaIsNull(articuloId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El artículo solicitado no existe");
        }
        return listaPrecioArticuloRepository.buscarListasPrecioPorArticulo(articuloId);
    }
}
