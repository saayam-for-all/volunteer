package org.sfa.volunteer.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record PaginationResponse<T>(
        int currentPage,
        int pageSize,
        int totalPages,
        long totalItems,
        List<T> items,
        boolean hasNextPage,
        boolean hasPreviousPage) {
}

