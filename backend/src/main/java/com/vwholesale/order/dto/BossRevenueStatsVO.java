package com.vwholesale.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BossRevenueStatsVO {

    private BigDecimal totalSales;
    private BigDecimal totalProfit;
    private BigDecimal totalPurchaseCost;
    private List<RevenueDailyRow> rows;
}
