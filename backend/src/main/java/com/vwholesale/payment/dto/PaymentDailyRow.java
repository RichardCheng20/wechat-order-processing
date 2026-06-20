package com.vwholesale.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PaymentDailyRow {

    private LocalDate date;
    private BigDecimal amount;
}
