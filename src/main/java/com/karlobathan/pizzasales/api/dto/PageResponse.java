package com.karlobathan.pizzasales.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * One page of results.
 *
 * @param content       the items on this page
 * @param page          zero-based page number
 * @param size          requested page size
 * @param totalElements number of items across all pages
 * @param totalPages    number of pages
 * @param <T>           item type
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
