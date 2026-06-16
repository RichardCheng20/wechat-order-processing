package com.vwholesale.product.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CategoryVO {

    private Long id;
    private Long parentId;
    private String name;
    private Integer sortOrder;
    private List<CategoryVO> children;
}
