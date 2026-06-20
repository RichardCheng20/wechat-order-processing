package com.vwholesale.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class RevenueDailyRow {

    private LocalDate date;
    private BigDecimal salesAmount;
    private BigDecimal purchaseCost;
    private BigDecimal profit;
}
