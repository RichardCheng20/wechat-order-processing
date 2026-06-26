package com.vwholesale.product.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InventoryReportRow {

    private Long productId;
    private Long categoryId;
    private String productName;
    private String unit;
    private BigDecimal inboundQty;
    private BigDecimal outboundQty;
    private BigDecimal stockQty;
    private BigDecimal availableQty;
}
