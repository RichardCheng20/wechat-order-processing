package com.vwholesale.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CustomerReportRow {

    private Long customerId;
    private String customerName;
    private BigDecimal receivableAmount;
    private BigDecimal receivedAmount;
}
