package com.vwholesale.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductRankingRow {

    private Long productId;
    private String productName;
    private Integer rank;
    private BigDecimal salesAmount;
    private BigDecimal purchaseCost;
    private BigDecimal profit;
}
