package com.vwholesale.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CustomerReportSummary {

    private BigDecimal receivableAmount;
    private BigDecimal receivedAmount;
    private BigDecimal discountAmount;
    private BigDecimal outstandingAmount;
    private BigDecimal refundAmount;
}
