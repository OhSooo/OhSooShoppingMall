package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response;

import lombok.Getter;

@Getter
public class CategoryResponse {

    private final Long id;
    private final String name;
    private final int depth;
    private final int displayOrder;

    public CategoryResponse(Long id, String name, int depth, int displayOrder) {
        this.id = id;
        this.name = name;
        this.depth = depth;
        this.displayOrder = displayOrder;
    }
}
