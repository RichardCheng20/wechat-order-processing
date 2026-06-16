package com.vwholesale.product.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductVO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String aliases;
    private String unit;
    private java.util.List<String> saleUnits;
    private String spec;
    private BigDecimal defaultPrice;
    private BigDecimal referencePrice;
    private String saleStatus;
}
