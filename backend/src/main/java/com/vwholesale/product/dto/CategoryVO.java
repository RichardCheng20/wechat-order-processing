package com.vwholesale.product.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryVO {

    private Long id;
    private String name;
    private Integer sortOrder;
}
