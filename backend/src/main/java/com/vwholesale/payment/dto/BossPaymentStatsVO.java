package com.vwholesale.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BossPaymentStatsVO {

    private BigDecimal totalReceived;
    private List<PaymentDailyRow> rows;
}
