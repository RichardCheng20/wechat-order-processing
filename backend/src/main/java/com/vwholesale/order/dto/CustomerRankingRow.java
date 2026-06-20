package com.vwholesale.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CustomerRankingRow {

    private Long customerId;
    private String customerName;
    private Integer rank;
    private BigDecimal salesAmount;
    private BigDecimal purchaseCost;
    private BigDecimal profit;
}
