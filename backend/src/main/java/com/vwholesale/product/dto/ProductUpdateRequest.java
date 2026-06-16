package com.vwholesale.product.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductUpdateRequest {

    private Long categoryId;
    private String name;
    private String aliases;
    private String unit;
    private java.util.List<String> saleUnits;
    private String spec;
    private BigDecimal defaultPrice;
    private String saleStatus;
}
