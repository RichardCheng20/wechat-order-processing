package com.vwholesale.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SupplierReportSummary {

    /** 区间内已付货款合计 */
    private BigDecimal paidAmount;
    /** 付款笔数 */
    private Integer paymentCount;
    /** 涉及供应商数 */
    private Integer supplierCount;
}
