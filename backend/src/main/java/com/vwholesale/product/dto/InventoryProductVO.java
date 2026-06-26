package com.vwholesale.product.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InventoryProductVO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String unit;
    private BigDecimal physicalStockQty;
    private BigDecimal reservedQty;
    private BigDecimal availableStockQty;
}
