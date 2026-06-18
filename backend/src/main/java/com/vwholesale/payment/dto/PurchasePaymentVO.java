package com.vwholesale.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PurchasePaymentVO {

    private Long id;
    private Long supplierId;
    private String supplierName;
    private BigDecimal amount;
    private String method;
    private LocalDateTime paidAt;
    private String remark;
    private List<String> voucherUrls;
    private LocalDateTime createdAt;
}
