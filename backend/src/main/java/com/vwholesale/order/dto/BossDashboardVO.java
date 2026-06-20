package com.vwholesale.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BossDashboardVO {

    private BigDecimal todaySales;
    private BigDecimal todayProfit;
    private BigDecimal todayPurchaseCost;
    private BigDecimal totalReceivable;
    private BigDecimal totalReceived;
    private BigDecimal totalPayable;
}
