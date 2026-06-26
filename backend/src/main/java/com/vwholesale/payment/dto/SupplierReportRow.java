package com.vwholesale.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SupplierReportRow {

    private Long supplierId;
    private String supplierName;
    private BigDecimal paidAmount;
    private Integer paymentCount;
}
