package com.tuckersoft.branchengine.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * La estructura de pagina del enunciado. No se devuelve el Page de Spring tal cual
 * porque su JSON trae otros nombres y mucho ruido.
 */
@NoArgsConstructor
@Getter
@Setter
public class PageResponse<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;

    public static <E, D> PageResponse<D> of(Page<E> page, Function<E, D> mapper) {
        PageResponse<D> respuesta = new PageResponse<>();
        respuesta.setContent(page.getContent().stream().map(mapper).toList());
        respuesta.setTotalElements(page.getTotalElements());
        respuesta.setTotalPages(page.getTotalPages());
        respuesta.setCurrentPage(page.getNumber());
        respuesta.setSize(page.getSize());
        return respuesta;
    }
}
