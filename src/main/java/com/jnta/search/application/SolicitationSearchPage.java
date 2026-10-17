package com.jnta.search.application;

import java.util.List;

public record SolicitationSearchPage(
        List<SolicitationSearchResult> items,
        long totalElements,
        int page,
        int size,
        int totalPages) {

    public SolicitationSearchPage(List<SolicitationSearchResult> items, long totalElements, int page, int size) {
        this(items, totalElements, page, size,
                totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size));
    }

    public SolicitationSearchPage {
        items = List.copyOf(items);
    }

    public static SolicitationSearchPage empty(int page, int size) {
        return new SolicitationSearchPage(List.of(), 0, page, size);
    }
}
